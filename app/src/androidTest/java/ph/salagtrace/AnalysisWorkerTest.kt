package ph.salagtrace

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import ph.salagtrace.domain.*
import ph.salagtrace.llm.*

class AnalysisWorkerTest {
    @Test fun realScheduledPipelineAndMissingModelState() = runBlocking {
        val c = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SalagApp).container
        c.models.rescan()
        val id = c.messages.accept("GCash sample ${System.nanoTime()}", SpikePrompt.BODY, System.currentTimeMillis(), Source.SAMPLE)
        val expected = if (c.models.active.value == null) AnalysisState.AI_UNAVAILABLE else AnalysisState.DONE
        val result = withTimeout(210_000) { c.database.messages().observeById(id).first { it?.state in setOf(AnalysisState.DONE, AnalysisState.AI_FAILED, AnalysisState.AI_UNAVAILABLE, AnalysisState.AI_TIMEOUT) } }!!
        assertEquals(result.error, expected, result.state)
        if (expected == AnalysisState.DONE) {
            assertTrue(result.promptTokens!! > 0); assertTrue(result.genTokens!! > 0)
            assertTrue(result.latencyMs!! > 0); assertNotNull(result.modelName)
            assertTrue(result.evidence().isNotEmpty())
            result.evidence().forEach { assertEquals(it.quote, result.maskedBody.substring(it.span.start, it.span.end)) }
        } else assertNull(result.modelName)
    }
}
