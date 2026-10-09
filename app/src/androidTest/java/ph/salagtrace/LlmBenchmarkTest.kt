package ph.salagtrace

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ph.salagtrace.llm.*
import java.io.File
import java.text.Normalizer

class LlmBenchmarkTest {
    @Test fun benchmarkProvisionedModels() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Enable explicitly with -e benchmark true", args.getString("benchmark") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fixtures = instrumentation.context.assets.open("eval_sms.jsonl").bufferedReader().readLines().map { Json.parseToJsonElement(it).jsonObject }
        val names = args.getString("models")?.split(',') ?: listOf("qwen2.5-1.5b-instruct-q4_k_m.gguf", "qwen2.5-0.5b-instruct-q4_k_m.gguf", "gemma-3-1b-it-Q4_K_M.gguf")
        val results = mutableListOf<JsonElement>()
        val totalStart = System.nanoTime()
        for (name in names) {
            if ((System.nanoTime() - totalStart) / 1_000_000_000 > 2700) break
            val file = File(context.getExternalFilesDir(null), "models/$name")
            if (!file.exists()) continue
            val start = System.nanoTime()
            val h = LlamaNative.nativeLoad(file.absolutePath, 1536, 2)
            val loadMs = (System.nanoTime() - start) / 1_000_000
            if (h == 0L) {
                results += buildJsonObject { put("model", name); put("loadFailed", true) }
                continue
            }
            try {
                val runs = fixtures.mapIndexed { index, f ->
                    val t = System.nanoTime()
                    val r = NativeResult.parse(LlamaNative.nativeComplete(h, SpikePrompt.SYSTEM,
                        "Sender: ${f.getValue("sender").jsonPrimitive.content}\n<sms>\n${f.getValue("body").jsonPrimitive.content}\n</sms>", ScamGrammar.GBNF, 320, 0.2f, 42, 90_000))
                    val elapsed = (System.nanoTime() - t) / 1_000_000
                    val obj = runCatching { Json.parseToJsonElement(r.text).jsonObject }.getOrNull()
                    val verdict = obj?.get("verdict")?.jsonPrimitive?.content ?: "parse_failure"
                    val correct = (verdict in listOf("likely_scam", "suspicious")) == f.getValue("scam").jsonPrimitive.boolean && verdict != "parse_failure"
                    val evidence = obj?.get("evidence")?.jsonArray.orEmpty()
                    val body = normalize(f.getValue("body").jsonPrimitive.content)
                    val verified = evidence.count { item ->
                        val q = normalize(item.jsonObject.getValue("quote").jsonPrimitive.content)
                        q.length >= 3 && body.contains(q)
                    }
                    Log.i("SALAG_BENCH", "model=$name sample=$index ms=$elapsed valid=${obj != null} correct=$correct prompt=${r.promptTokens} gen=${r.genTokens}")
                    buildJsonObject {
                        put("id", f.getValue("id")); put("latencyMs", elapsed); put("validJson", obj != null); put("correct", correct)
                        put("verdict", verdict); put("stop", r.stop); put("promptTokens", r.promptTokens); put("genTokens", r.genTokens)
                        put("prefillMs", r.prefillMs); put("genMs", r.genMs); put("evidenceCount", evidence.size); put("verifiedExactQuotes", verified)
                        put("rssKb", File("/proc/self/status").readLines().firstOrNull { it.startsWith("VmRSS:") }?.filter { it.isDigit() }?.toLongOrNull() ?: 0)
                    }
                }
                results += buildJsonObject { put("model", name); put("loadMs", loadMs); put("runs", JsonArray(runs)) }
                File(context.getExternalFilesDir(null), "benchmarks.json").writeText(JsonArray(results).toString())
            } finally { LlamaNative.nativeFree(h) }
        }
        assumeTrue("No models provisioned", results.isNotEmpty())
    }
    private fun normalize(text: String) = Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase().replace(Regex("\\s+"), " ").trim()
}
