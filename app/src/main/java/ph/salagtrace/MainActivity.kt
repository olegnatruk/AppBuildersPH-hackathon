package ph.salagtrace

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ph.salagtrace.llm.*
import java.io.File

class MainActivity : ComponentActivity() {
    private var handle = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val models = File(getExternalFilesDir(null), "models").apply { mkdirs() }
        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                var result by remember { mutableStateOf("Model not loaded") }
                var busy by remember { mutableStateOf(false) }
                val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
                Column(Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("SALAG TRACE · Technical spike", style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = { permissions.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS)) }) { Text("Allow SMS & alerts") }
                    Button(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true
                            result = withContext(Dispatchers.Default) {
                                val model = File(models, "qwen2.5-1.5b-instruct-q4_k_m.gguf")
                                if (!model.exists()) "Copy model into $models" else {
                                    if (handle != 0L) LlamaNative.nativeFree(handle)
                                    val start = System.nanoTime()
                                    handle = LlamaNative.nativeLoad(model.absolutePath, 1536, (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, 4))
                                    "Load: ${(System.nanoTime() - start) / 1_000_000} ms; handle ready=${handle != 0L}"
                                }
                            }
                            busy = false
                        }
                    }) { Text("Load real model") }
                    Button(enabled = !busy && handle != 0L, onClick = {
                        scope.launch {
                            busy = true
                            result = "Running locally…"
                            result = withContext(Dispatchers.Default) {
                                val r = NativeResult.parse(LlamaNative.nativeComplete(handle, SpikePrompt.SYSTEM, "<sms>\n${SpikePrompt.BODY}\n</sms>", ScamGrammar.GBNF, 320, 0.2f, 42, 90_000))
                                "${r.text}\n\nStop: ${r.stop}\nPrompt: ${r.promptTokens}, generated: ${r.genTokens}\nPrefill: ${r.prefillMs.toLong()} ms · generation: ${r.genMs.toLong()} ms\n${r.error}"
                            }
                            busy = false
                        }
                    }) { Text("Run grammar-constrained test") }
                    Text(result)
                }
            }
        }
    }
}
