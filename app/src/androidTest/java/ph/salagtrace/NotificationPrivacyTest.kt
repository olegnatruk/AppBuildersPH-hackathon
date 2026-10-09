package ph.salagtrace

import android.app.Notification
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ph.salagtrace.data.ScannedMessageEntity
import ph.salagtrace.domain.*

class NotificationPrivacyTest {
    @Test fun privateAlertAndForegroundNotificationHaveSeparateIdentities() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SalagApp
        val manager = app.getSystemService(NotificationManager::class.java)
        assumeTrue(manager.areNotificationsEnabled())
        val m = ScannedMessageEntity(id=1,dedupKey="notification-test",source=Source.SMS,sender="09170001111",maskedBody="Synthetic test content must never enter an alert",maskSpansJson="[]",receivedAt=System.currentTimeMillis(),ruleScore=70,ruleSignalsJson="[]",urlsJson="[]",state=AnalysisState.SCREENED,verdict=Verdict.LIKELY_SCAM)
        try {
            manager.notify(1001,Notification.Builder(app,"scam_alerts").setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("Notification regression").build())
            app.container.notifier.screened(m,AppLanguage.ENGLISH)
            val alert = manager.activeNotifications.first { it.id==1001 && it.tag=="sms-1" }.notification
            assertTrue(manager.activeNotifications.any { it.id==1001 && it.tag==null })
            assertEquals(Notification.VISIBILITY_PRIVATE,alert.visibility)
            val body = alert.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
            assertEquals("0917•••1111 · Rule check",body);assertFalse(body.contains(m.maskedBody))
            assertNotNull(alert.publicVersion);assertNotNull(alert.contentIntent)
            app.container.notifier.cancel(1)
            assertTrue(manager.activeNotifications.any { it.id==1001 && it.tag==null })
        } finally { app.container.notifier.cancel(1);manager.cancel(1001) }
    }
}
