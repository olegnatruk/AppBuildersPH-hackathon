package ph.salagtrace

import android.Manifest
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ph.salagtrace.domain.*

class ReceiverPipelineTest {
    @Test fun realBroadcastPersistsOnlyMaskedText() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("sms") == "true")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SalagApp
        val c = app.container
        c.settings.consent(); c.settings.setMonitoring(true)
        val before = c.database.messages().observeAll().first().map { it.id }.toSet()
        Log.i("SALAG_TEST", "receiver_ready")
        try {
            val row = withTimeout(30_000) {
                c.database.messages().observeAll().first { rows -> rows.any { it.id !in before && it.source == Source.SMS } }.first { it.id !in before && it.source == Source.SMS }
            }
            assertTrue(row.maskedBody.contains("[OTP]"))
            assertFalse(row.maskedBody.contains("937152"))
            assertTrue(row.masks().isNotEmpty())
            assertTrue(row.state in setOf(AnalysisState.QUEUED_AI, AnalysisState.ANALYZING, AnalysisState.DONE))
        } finally { c.settings.setMonitoring(false) }
    }
}
