package ph.salagtrace.notify

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import ph.salagtrace.MainActivity
import ph.salagtrace.R
import ph.salagtrace.data.ScannedMessageEntity
import ph.salagtrace.domain.*

/** No SMS body, quote, URL, or AI explanation may enter a notification. */
class AlertNotifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    init {
        manager.createNotificationChannel(NotificationChannel("scam_alerts", "Scam warnings", NotificationManager.IMPORTANCE_HIGH).apply { description = "Private warnings for new SMS"; lockscreenVisibility = Notification.VISIBILITY_PRIVATE })
    }
    fun screened(m: ScannedMessageEntity, language: AppLanguage) { if(m.ruleScore >= 70) post(m,language,false) }
    fun analyzed(m: ScannedMessageEntity, language: AppLanguage) {
        if(m.verdict in setOf(Verdict.LIKELY_SCAM,Verdict.SUSPICIOUS)) post(m,language,m.isAiResult()) else cancel(m.id)
    }
    private fun post(m: ScannedMessageEntity, language: AppLanguage, ai: Boolean) {
        if(m.source != Source.SMS || !manager.areNotificationsEnabled() || (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)) return
        val fil = language == AppLanguage.FILIPINO
        val title = if(fil) "Kahina-hinalang bagong SMS" else "Suspicious new SMS"
        val sender = if(m.sender.count { it.isDigit() } >= 7) m.sender.take(4) + "•••" + m.sender.takeLast(4) else m.sender.take(32)
        val line = "$sender · ${if(ai) { if(fil) "AI sa phone" else "On-device AI" } else "Rule check"}"
        val intent = Intent(context, MainActivity::class.java).putExtra("messageId",m.id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context,id(m.id),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val public = Notification.Builder(context,"scam_alerts").setSmallIcon(R.drawable.ic_notification).setContentTitle("SALAG TRACE").setContentText(if(fil) "Buksan ang app para suriin." else "Open the app to investigate.").build()
        val notification = Notification.Builder(context,"scam_alerts").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(line)
            .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(public).setCategory(Notification.CATEGORY_MESSAGE).build()
        try { manager.notify("sms-${m.id}",id(m.id),notification) } catch (_: SecurityException) { /* Revocation can race the permission check. */ }
    }
    private fun id(value: Long) = (value % (Int.MAX_VALUE - 1000)).toInt() + 1000
    fun cancel(id: Long) { manager.cancel("sms-$id",id(id)) }
    fun cancelAll() { manager.cancelAll() }
}
