package ph.salagtrace

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ph.salagtrace.domain.*
import ph.salagtrace.data.*
import ph.salagtrace.work.AnalysisWorker
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.serialization.encodeToString
import ph.salagtrace.llm.*
import java.io.File

class FailureStateTest {
    private fun container() = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SalagApp).container
    @Test fun missingModelKeepsRuleResult() = runBlocking {
        val c = container(); c.models.rescan(); c.engine.unload()
        val renamed = c.models.models.value.map { it to File(it.parentFile, it.name + ".held") }
        try {
            renamed.forEach { (f, temp) -> assertTrue(f.renameTo(temp)) }; c.models.rescan()
            val id = c.messages.accept("Missing model ${System.nanoTime()}", SpikePrompt.BODY, System.currentTimeMillis(), Source.SAMPLE)
            val row = withTimeout(20_000) { c.database.messages().observeById(id).first { it?.state == AnalysisState.AI_UNAVAILABLE } }!!
            assertNull(row.modelName); assertFalse(row.isAiResult()); assertTrue(row.ruleScore >= 25); assertNotNull(row.error)
        } finally { renamed.forEach { (f, temp) -> if (temp.exists()) temp.renameTo(f) }; c.models.rescan() }
    }
    @Test fun nativeTimeoutAndCancelAreBounded() = runBlocking {
        val c = container(); val file = c.models.activeModel(); assumeTrue("Real model needed", file != null)
        val p = PromptBuilder.build(SpikePrompt.BODY, "test", emptyList(), AppLanguage.ENGLISH)
        val timed = c.engine.complete(file!!, p.system, p.user, -2, timeoutMs = 1)
        assertEquals("timeout", timed.result.stop)
        val job = async { c.engine.complete(file, p.system, p.user, -3) }
        withTimeout(5_000) { c.engine.state.first { it is EngineState.Busy && it.messageId == -3L } }
        delay(100); c.engine.cancel(-3)
        assertEquals("cancel", withTimeout(5_000) { job.await() }.result.stop)
    }

    @Test fun workerTimeoutAndParseFailureRemainRuleChecks() = runBlocking {
        val c = container(); assumeTrue(c.models.activeModel() != null)
        val body = Masker.mask(SpikePrompt.BODY).body; val rules = Screener.screen(body)
        suspend fun runCase(timeout: Long, tokens: Int, expected: AnalysisState) {
            val row = ScannedMessageEntity(dedupKey = "failure-${System.nanoTime()}", source = Source.SAMPLE, sender = "Test", maskedBody = body,
                maskSpansJson = "[]", receivedAt = System.currentTimeMillis(), ruleScore = rules.score,
                ruleSignalsJson = StorageJson.encodeToString(rules.signals), urlsJson = StorageJson.encodeToString(rules.urls),
                state = AnalysisState.QUEUED_AI, verdict = Screener.ruleVerdict(rules))
            val id = c.database.messages().insertIgnore(row)
            val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SalagApp
            val worker = TestListenableWorkerBuilder<AnalysisWorker>(app)
                .setWorkerFactory(object : WorkerFactory() {
                    override fun createWorker(appContext: android.content.Context, workerClassName: String, parameters: WorkerParameters): ListenableWorker = AnalysisWorker(app, parameters)
                })
                .setInputData(workDataOf("messageId" to id, "timeoutMs" to timeout, "maxTokens" to tokens)).build()
            worker.doWork()
            val result = c.database.messages().byId(id)!!
            assertEquals(result.error, expected, result.state); assertFalse(result.isAiResult()); assertNull(result.modelName)
        }
        runCase(1, 320, AnalysisState.AI_TIMEOUT)
        runCase(90_000, 1, AnalysisState.AI_FAILED)
    }
    @Test fun corruptGgufFailsWithoutCrash() = runBlocking {
        val c = container()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "corrupt.gguf")
        java.io.RandomAccessFile(file, "rw").use { it.writeBytes("GGUF"); it.setLength(100_000_001) }
        try {
            c.engine.unload()
            val p = PromptBuilder.build("hello", "test", emptyList(), AppLanguage.ENGLISH)
            try { c.engine.complete(file, p.system, p.user, -4); fail("Corrupt model was accepted") } catch (_: ModelLoadException) { }
        } finally { file.delete() }
    }
}
class ResetDemoStateTest {
    @Test fun resetOnlyWhenExplicitlyRequested() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("reset") == "true")
        val c = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SalagApp).container
        c.engine.cancel(); c.scheduler.cancelAll(); c.database.messages().deleteAll(); c.settings.resetConsent()
    }
}
