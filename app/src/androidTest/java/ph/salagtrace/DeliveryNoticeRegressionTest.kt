package ph.salagtrace

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ph.salagtrace.data.*
import ph.salagtrace.domain.*
import java.io.File

/** Explicit opt-in: real production workers, including ALL scope and a formerly stored rule screen. */
class DeliveryNoticeRegressionTest {
    @Test fun codAndDangerousVariantUseRealProductionPipeline() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("courier") == "true")
        val i = InstrumentationRegistry.getInstrumentation()
        val c = (i.targetContext.applicationContext as SalagApp).container
        assumeTrue("Provision a real GGUF", c.models.activeModel() != null)
        val body = i.context.assets.open("cod_delivery.txt").bufferedReader().use { it.readText().trim() }
        val old = c.settings.settings.first()
        val ids = mutableListOf<Long>(); val results = mutableListOf<JsonElement>()
        suspend fun await(id: Long): ScannedMessageEntity = withTimeout(240_000) {
            c.database.messages().observeById(id).first { it != null && it.state !in setOf(AnalysisState.QUEUED_AI, AnalysisState.ANALYZING) }
        }!!
        fun record(label: String, row: ScannedMessageEntity) {
            results += buildJsonObject {
                put("case", label); put("state", row.state.name); put("verdict", row.verdict.wire)
                put("model", row.modelName ?: ""); put("latencyMs", row.latencyMs ?: 0)
                put("promptTokens", row.promptTokens ?: 0); put("evidenceCount", row.evidence().size)
                put("disagreement", row.disagreement)
            }
            File(i.targetContext.getExternalFilesDir(null), "courier-regression.json").writeText(JsonArray(results).toString())
        }
        suspend fun accept(text: String) = c.messages.accept("Courier regression ${System.nanoTime()}", text, System.currentTimeMillis(), Source.SAMPLE).also { ids += it }
        try {
            c.settings.setAiScope(AiScope.FLAGGED)
            val screened = await(accept(body))
            assertEquals(AnalysisState.SCREENED, screened.state); assertEquals(Verdict.LIKELY_SAFE, screened.verdict)
            assertFalse(screened.isAiResult()); assertTrue(screened.tactics().isEmpty()); record("flagged_cod", screened)

            c.settings.setAiScope(AiScope.ALL)
            for (language in AppLanguage.entries) {
                c.settings.setLanguage(language)
                val row = await(accept(body))
                assertEquals(row.error, AnalysisState.DONE, row.state); assertTrue(row.isAiResult())
                assertTrue(row.verdict in setOf(Verdict.LIKELY_SAFE, Verdict.UNCERTAIN)); record("all_${language.name.lowercase()}", row)
            }

            c.settings.setLanguage(AppLanguage.ENGLISH); c.settings.setAiScope(AiScope.FLAGGED)
            val masked = Masker.mask(body)
            val payStart = masked.body.indexOf("pay")
            val legacy = ScannedMessageEntity(dedupKey = "old-cod-${System.nanoTime()}", source = Source.SAMPLE, sender = "Courier regression",
                maskedBody = masked.body, maskSpansJson = StorageJson.encodeToString(masked.spans), receivedAt = System.currentTimeMillis(), ruleScore = 40,
                ruleSignalsJson = StorageJson.encodeToString(listOf(RuleSignal("payment_pay", TextSpan(payStart, payStart + 3), Tactic.PAYMENT_REQUEST))),
                urlsJson = "[]", state = AnalysisState.SCREENED, verdict = Verdict.SUSPICIOUS)
            val legacyId = c.database.messages().insertIgnore(legacy).also { ids += it }
            c.messages.rerun(legacyId)
            val corrected = await(legacyId)
            assertEquals(corrected.error, AnalysisState.DONE, corrected.state)
            assertTrue(RoutineDeliveryContext.matches(corrected.rules()))
            assertTrue(corrected.verdict in setOf(Verdict.LIKELY_SAFE, Verdict.UNCERTAIN)); record("rerun_old_rules", corrected)

            val dangerous = await(accept("$body\nPay an advance redelivery fee now at https://parcel-pay.xyz/release."))
            assertEquals(dangerous.error, AnalysisState.DONE, dangerous.state); assertTrue(dangerous.isAiResult())
            assertFalse(RoutineDeliveryContext.matches(dangerous.rules()))
            assertTrue(dangerous.verdict in setOf(Verdict.LIKELY_SCAM, Verdict.SUSPICIOUS)); record("phishing_variant", dangerous)
        } finally {
            ids.forEach { c.scheduler.cancel(it); c.database.messages().deleteById(it) }
            c.settings.setLanguage(old.language); c.settings.setAiScope(old.aiScope)
        }
    }
}
