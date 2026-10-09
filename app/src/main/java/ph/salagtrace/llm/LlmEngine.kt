package ph.salagtrace.llm

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ph.salagtrace.util.SafeLog
import java.io.File

sealed interface EngineState {
    data object Unloaded : EngineState
    data object Loading : EngineState
    data class Ready(val model: String) : EngineState
    data class Busy(val model: String, val messageId: Long) : EngineState
    data class Error(val reason: String) : EngineState
}
data class LlmCompletion(val result: NativeResult, val modelName: String, val loadMs: Long, val latencyMs: Long)
class ModelLoadException : Exception("Model could not load. Check the GGUF file and available memory.")

/** All load/decode/free operations share one mutex. Cancellation only touches the native atomic flag. */
class LlmEngine(private val scope: CoroutineScope) {
    private val mutex = Mutex()
    private val lifetime = Any()
    @Volatile private var handle = 0L
    @Volatile private var currentOwner: Long? = null
    @Volatile private var cancelledOwner: Long? = null
    private var path = ""
    private var idleJob: Job? = null
    private val _state = MutableStateFlow<EngineState>(EngineState.Unloaded)
    val state: StateFlow<EngineState> = _state.asStateFlow()

    suspend fun complete(file: File, system: String, user: String, ownerId: Long,
        timeoutMs: Long = 90_000, temperature: Float = 0.2f, onStarted: suspend () -> Unit = {}): LlmCompletion = mutex.withLock {
        idleJob?.cancel(); currentOwner = ownerId; cancelledOwner = null
        val callerJob = currentCoroutineContext()[Job]
        val start = System.nanoTime()
        var watchdog: Job? = null
        try {
            onStarted()
            val loadMs = withContext(Dispatchers.Default) { ensureLoadedLocked(file) }
            callerJob?.ensureActive()
            _state.value = EngineState.Busy(file.name, ownerId)
            watchdog = scope.launch {
                while (isActive) {
                    delay(50)
                    if (callerJob?.isActive == false || cancelledOwner == ownerId) cancel(ownerId)
                }
            }
            // Retain the mutex until JNI really returns, even if WorkManager stops this coroutine.
            val result = withContext(NonCancellable + Dispatchers.Default) {
                NativeResult.parse(LlamaNative.nativeComplete(handle, system, user, ScamGrammar.GBNF, 320, temperature, 42, timeoutMs))
            }
            callerJob?.ensureActive()
            val latency = (System.nanoTime() - start) / 1_000_000
            SafeLog.inference(result.promptTokens, result.genTokens, latency)
            _state.value = EngineState.Ready(file.name)
            LlmCompletion(if (cancelledOwner == ownerId) result.copy(text = "", stop = "cancel") else result, file.name, loadMs, latency)
        } catch (e: CancellationException) { _state.value = if (handle != 0L) EngineState.Ready(file.name) else EngineState.Unloaded; throw e }
        catch (e: Exception) { _state.value = EngineState.Error("Local AI could not run. Check the model and available memory."); throw e }
        finally {
            watchdog?.cancel(); currentOwner = null; cancelledOwner = null
            idleJob = scope.launch { delay(120_000); unloadIfIdle() }
        }
    }
    private fun ensureLoadedLocked(file: File): Long {
        if (handle != 0L && path == file.absolutePath) return 0
        freeLocked(); _state.value = EngineState.Loading
        val t = System.nanoTime()
        val loaded = LlamaNative.nativeLoad(file.absolutePath, 1536, (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 4))
        if (loaded == 0L) throw ModelLoadException()
        synchronized(lifetime) { handle = loaded; path = file.absolutePath }
        return (System.nanoTime() - t) / 1_000_000
    }
    fun cancel(messageId: Long? = null) {
        synchronized(lifetime) {
            if (currentOwner != null && (messageId == null || currentOwner == messageId)) {
                cancelledOwner = currentOwner
                if (handle != 0L) LlamaNative.nativeCancel(handle)
            }
        }
    }
    suspend fun unload() = mutex.withLock { idleJob?.cancel(); freeLocked() }
    suspend fun unloadIfIdle() {
        if (!mutex.tryLock()) return
        try { if (currentOwner == null) freeLocked() } finally { mutex.unlock() }
    }
    private fun freeLocked() {
        synchronized(lifetime) {
            if (handle != 0L) LlamaNative.nativeFree(handle)
            handle = 0; path = ""
        }
        _state.value = EngineState.Unloaded
    }
}
