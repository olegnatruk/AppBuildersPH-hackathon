package ph.salagtrace

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ph.salagtrace.domain.*
import ph.salagtrace.llm.EngineState
import ph.salagtrace.llm.SpikePrompt
import java.io.File

/** Host kills the real process only after the native Busy signal. Verification is a separate invocation. */
class ProcessRecoveryTest {
    @Test fun prepareRunningAnalysisForHostCrash(): Unit = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("prepareRecovery") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SalagApp
        val c = app.container; assumeTrue(c.models.activeModel() != null)
        val id = c.messages.accept("Recovery ${System.nanoTime()}",SpikePrompt.BODY,System.currentTimeMillis(),Source.SAMPLE)
        File(app.getExternalFilesDir(null),"recovery-id.txt").writeText(id.toString())
        withTimeout(120_000) { c.engine.state.first { it is EngineState.Busy && it.messageId == id } }
        delay(150)
        Log.i("SALAG_TEST","recovery_native_busy id=$id")
        delay(30_000)
        error("Host did not interrupt the native-busy process")
    }
    @Test fun reopenedProcessCompletesPersistedAnalysis() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("verifyRecovery") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SalagApp
        val c = app.container; val id = File(app.getExternalFilesDir(null),"recovery-id.txt").readText().toLong()
        c.scheduler.recover(c.database.messages())
        val m = withTimeout(220_000) { c.database.messages().observeById(id).first { it?.state in setOf(AnalysisState.DONE,AnalysisState.AI_FAILED,AnalysisState.AI_TIMEOUT,AnalysisState.AI_UNAVAILABLE) } }!!
        assertEquals(m.error,AnalysisState.DONE,m.state);assertTrue(m.isAiResult());assertTrue(m.latencyMs!! > 0);assertTrue(m.evidence().isNotEmpty())
        c.database.messages().deleteById(id)
    }
}
