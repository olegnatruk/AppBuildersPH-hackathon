package ph.salagtrace

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ph.salagtrace.data.*
import ph.salagtrace.domain.*
import ph.salagtrace.llm.*
import ph.salagtrace.work.AnalysisWorker

class StoppedWorkerTest {
    @Test fun stoppedQueuedWorkerCanResumeButExplicitCancelCannot() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SalagApp
        val c = app.container
        val file = c.models.activeModel(); assumeTrue(file != null)
        val body = Masker.mask(SpikePrompt.BODY).body; val rules = Screener.screen(body)
        val id = c.database.messages().insertIgnore(ScannedMessageEntity(dedupKey="stop-${System.nanoTime()}",source=Source.SAMPLE,sender="Stop test",maskedBody=body,maskSpansJson="[]",receivedAt=System.currentTimeMillis(),ruleScore=rules.score,ruleSignalsJson=StorageJson.encodeToString(rules.signals),urlsJson=StorageJson.encodeToString(rules.urls),state=AnalysisState.QUEUED_AI,verdict=Screener.ruleVerdict(rules)))
        val prompt = PromptBuilder.build(body,"test",rules.signals,AppLanguage.ENGLISH)
        val holder = async { c.engine.complete(file!!,prompt.system,prompt.user,-9) }
        try {
            withTimeout(20_000) { c.engine.state.first { it is EngineState.Busy && it.messageId == -9L } }
            val worker = TestListenableWorkerBuilder<AnalysisWorker>(app).setWorkerFactory(object: WorkerFactory() {
                override fun createWorker(appContext: Context,workerClassName: String,workerParameters: WorkerParameters): ListenableWorker = AnalysisWorker(app,workerParameters)
            }).setInputData(workDataOf("messageId" to id)).build()
            val waiting = launch { worker.doWork() }
            delay(250); waiting.cancelAndJoin()
            assertEquals(AnalysisState.QUEUED_AI,c.database.messages().byId(id)!!.state)
            val row = c.database.messages().byId(id)!!
            c.database.messages().update(row.copy(state=AnalysisState.AI_FAILED,error="Analysis canceled. Rule check remains available."))
            assertEquals(0,c.database.messages().requeueStopped(id))
            assertEquals(AnalysisState.AI_FAILED,c.database.messages().byId(id)!!.state)
        } finally {
            c.engine.cancel(-9); withTimeout(10_000) { holder.await() }; c.database.messages().deleteById(id)
        }
    }
}
