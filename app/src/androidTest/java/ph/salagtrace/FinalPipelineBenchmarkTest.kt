package ph.salagtrace

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ph.salagtrace.domain.*
import java.io.File

/** Explicit opt-in: runs all fixtures through real production gating, masking, worker and fusion. */
class FinalPipelineBenchmarkTest {
    @Test fun measuredFinalPipeline() = runBlocking {
        val i = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("evaluate") == "true")
        val c = (i.targetContext.applicationContext as SalagApp).container
        c.models.rescan(); assumeTrue(c.models.active.value != null)
        val old = c.settings.settings.first()
        c.settings.setLanguage(AppLanguage.FILIPINO); c.settings.setAiScope(AiScope.FLAGGED)
        val fixtures = i.context.assets.open("eval_sms.jsonl").bufferedReader().readLines().map { Json.parseToJsonElement(it).jsonObject }
        val results = mutableListOf<JsonElement>()
        try {
            fixtures.forEach { f ->
                val id = c.messages.accept("eval-${f.getValue("id").jsonPrimitive.content}",f.getValue("body").jsonPrimitive.content,System.currentTimeMillis(),Source.MANUAL)
                val m = withTimeout(220_000) { c.database.messages().observeById(id).first { it?.state !in setOf(null,AnalysisState.QUEUED_AI,AnalysisState.ANALYZING) } }!!
                val scam = f.getValue("scam").jsonPrimitive.boolean
                results += buildJsonObject {
                    put("id",f.getValue("id")); put("expectedScam",scam); put("verdict",m.verdict.wire); put("state",m.state.name)
                    put("correct",(m.verdict in setOf(Verdict.LIKELY_SCAM,Verdict.SUSPICIOUS)) == scam)
                    put("latencyMs",m.latencyMs ?: 0); put("evidenceCount",m.evidence().size); put("droppedEvidence",m.droppedEvidence)
                    put("disagreement",m.disagreement); put("language",m.explanationLang ?: "RULE_CHECK")
                }
                File(i.targetContext.getExternalFilesDir(null),"final-pipeline.json").writeText(JsonArray(results).toString())
                c.database.messages().deleteById(id)
            }
        } finally { c.settings.setLanguage(old.language); c.settings.setAiScope(old.aiScope) }
    }
}
