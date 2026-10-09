package ph.salagtrace.llm

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import ph.salagtrace.data.SettingsRepository
import java.io.File
import java.io.IOException

sealed interface ImportState {
    data object Idle : ImportState
    data class Copying(val bytes: Long, val total: Long) : ImportState
    data class Done(val name: String) : ImportState
    data class Error(val reason: String) : ImportState
}
class ModelManager(private val context: Context, private val settings: SettingsRepository, private val onChange: suspend () -> Unit) {
    companion object { const val DEFAULT_MODEL = "qwen2.5-1.5b-instruct-q4_k_m.gguf" }
    private val internal = File(context.filesDir, "models").apply { mkdirs() }
    private val external = context.getExternalFilesDir(null)?.let { File(it, "models").apply { mkdirs() } }
    private val importMutex = Mutex()
    private val _models = MutableStateFlow<List<File>>(emptyList())
    val models: StateFlow<List<File>> = _models.asStateFlow()
    private val _import = MutableStateFlow<ImportState>(ImportState.Idle)
    val importing: StateFlow<ImportState> = _import.asStateFlow()
    private val _active = MutableStateFlow<File?>(null)
    val active: StateFlow<File?> = _active.asStateFlow()

    fun isValid(file: File): Boolean = runCatching {
        file.isFile && file.length() > 100_000_000 && file.inputStream().use { input ->
            val header = ByteArray(4); input.read(header) == 4 && header.contentEquals(byteArrayOf(71, 71, 85, 70))
        }
    }.getOrDefault(false)
    suspend fun rescan() = withContext(Dispatchers.IO) {
        val found = listOfNotNull(internal, external).flatMap { it.listFiles().orEmpty().toList() }
            .filter { it.extension.equals("gguf", true) && isValid(it) }.sortedWith(compareBy<File> { it.name != DEFAULT_MODEL }.thenBy { it.name })
        _models.value = found
        val preferred = settings.settings.first().activeModel
        val chosen = found.firstOrNull { it.absolutePath == preferred } ?: found.firstOrNull()
        _active.value = chosen
        if (chosen != null && chosen.absolutePath != preferred) settings.setActiveModel(chosen.absolutePath)
    }
    suspend fun select(file: File) {
        require(_models.value.any { it.absolutePath == file.absolutePath } && isValid(file))
        onChange(); settings.setActiveModel(file.absolutePath); _active.value = file
    }
    suspend fun activeModel(): File? { rescan(); return _active.value }
    suspend fun removeActive() {
        val file = _active.value ?: return
        onChange()
        withContext(Dispatchers.IO) { if (!file.delete()) throw IOException("Model file could not be removed") }
        settings.setActiveModel(""); rescan()
    }
    suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        if (!importMutex.tryLock()) return@withContext
        var temporary: File? = null
        try {
            var name = "imported-model.gguf"; var total = -1L
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val n = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (n >= 0) name = cursor.getString(n) ?: name
                    val s = cursor.getColumnIndex(OpenableColumns.SIZE); if (s >= 0 && !cursor.isNull(s)) total = cursor.getLong(s)
                }
            }
            if (!name.endsWith(".gguf", true)) throw IOException("Choose a .gguf file")
            name = name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120)
            if (!name.endsWith(".gguf", true)) name += ".gguf"
            if (total > 0 && total + 50_000_000 > internal.usableSpace) throw IOException("Not enough free storage for this model")
            temporary = File(internal, "$name.importing")
            _import.value = ImportState.Copying(0, total)
            context.contentResolver.openInputStream(uri)?.use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(1024 * 1024); var bytes = 0L
                    while (true) {
                        val n = input.read(buffer); if (n < 0) break
                        output.write(buffer, 0, n); bytes += n; _import.value = ImportState.Copying(bytes, total)
                    }
                }
            } ?: throw IOException("Could not open the selected file")
            if (!isValid(temporary)) throw IOException("Not a complete GGUF model (minimum 100 MB)")
            onChange()
            val destination = File(internal, name)
            if (!temporary.renameTo(destination)) throw IOException("Could not finish model import")
            settings.setActiveModel(destination.absolutePath); rescan(); _import.value = ImportState.Done(name)
        } catch (_: Exception) { _import.value = ImportState.Error("Import failed. Choose a complete GGUF over 100 MB and check free storage.") }
        finally { temporary?.delete(); importMutex.unlock() }
    }
}
