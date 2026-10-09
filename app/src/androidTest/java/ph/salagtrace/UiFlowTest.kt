package ph.salagtrace

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import ph.salagtrace.domain.*

class UiFlowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun consentGateDeniedPermissionPathAndMaskedManualRecord() {
        val c = (ui.activity.application as SalagApp).container
        runBlocking { c.settings.resetConsent(); c.settings.setLanguage(AppLanguage.ENGLISH) }
        ui.waitUntil(10_000) { ui.onAllNodesWithText("Continue").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Continue").performClick()
        ui.onNodeWithText("Continue").assertIsNotEnabled()
        ui.onNode(isToggleable()).performClick()
        ui.onNodeWithText("Continue").performClick()
        ui.onNodeWithText("Finish setup").performScrollTo().performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithText("Home").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(runBlocking { c.settings.settings.first().consentAt > 0 })
        ui.onNode(hasScrollAction()).performScrollToNode(hasText("Check a message"))
        ui.onNodeWithText("Check a message").performClick()
        ui.onNodeWithText("Message").performTextInput("Your OTP is 937152. Do not share this code.")
        ui.onNodeWithText("Analyze on device").performScrollTo().performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithText("Message trace").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("MANUAL").assertExists()
        ui.onNodeWithText("Your OTP is [OTP]",substring = true).assertExists()
        val row = runBlocking { c.database.messages().observeAll().first().first { it.source == Source.MANUAL && it.maskedBody.contains("Do not share this code") } }
        assertFalse(row.maskedBody.contains("937152")); assertFalse(row.isAiResult())
        // Carrier inbox is never touched; remove only the test-created app record.
        runBlocking { c.database.messages().deleteById(row.id) }
    }
}
