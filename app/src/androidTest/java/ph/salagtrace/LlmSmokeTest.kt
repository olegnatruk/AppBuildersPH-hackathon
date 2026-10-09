package ph.salagtrace

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlinx.serialization.json.*
import ph.salagtrace.llm.*
import java.io.File
import android.util.Log

class LlmSmokeTest {
    @Test fun realModelProducesGrammarConstrainedJson() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "models/qwen2.5-1.5b-instruct-q4_k_m.gguf")
        assumeTrue("Provision a real GGUF first", file.exists())
        val start = System.nanoTime()
        val h = LlamaNative.nativeLoad(file.absolutePath, 1536, 2)
        assertNotEquals("Model failed to load", 0L, h)
        val loadMs = (System.nanoTime() - start) / 1_000_000
        try {
            val r = NativeResult.parse(LlamaNative.nativeComplete(h, SpikePrompt.SYSTEM, "<sms>\n${SpikePrompt.BODY}\n</sms>", ScamGrammar.GBNF, 320, 0.2f, 42, 90_000))
            assertEquals(r.error, "eos", r.stop)
            val obj = Json.parseToJsonElement(r.text).jsonObject
            assertTrue(obj.getValue("verdict").jsonPrimitive.content in listOf("likely_scam", "suspicious", "uncertain", "likely_safe"))
            assertTrue(r.genTokens > 0)
            assertTrue(r.prefillMs > 0)
            Log.i("SALAG_BENCH", "smoke loadMs=$loadMs prompt=${r.promptTokens} gen=${r.genTokens} prefillMs=${r.prefillMs} genMs=${r.genMs} verdict=${obj.getValue("verdict").jsonPrimitive.content}")
            File(context.getExternalFilesDir(null), "smoke.json").writeText(buildJsonObject {
                put("loadMs", loadMs); put("promptTokens", r.promptTokens); put("genTokens", r.genTokens)
                put("prefillMs", r.prefillMs); put("genMs", r.genMs); put("stop", r.stop)
                put("verdict", obj.getValue("verdict"))
            }.toString())
        } finally { LlamaNative.nativeFree(h) }
    }
}
