package ph.salagtrace.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ph.salagtrace.SalagApp
import ph.salagtrace.domain.Source
import ph.salagtrace.util.SafeLog

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        val container = (context.applicationContext as SalagApp).container
        container.scope.launch {
            try {
                withTimeout(8_000) {
                    val settings = container.settings.settings.first()
                    if (!settings.monitoringEnabled || settings.consentAt == 0L) return@withTimeout
                    val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent).map { SmsPart(it.originatingAddress, it.messageBody.orEmpty(), it.timestampMillis) }
                    SmsAssembler.assemble(parts).forEach { sms ->
                        val id = container.messages.accept(sms.sender, sms.body, sms.timestamp, Source.SMS)
                        SafeLog.received(id, sms.body.length)
                    }
                }
            } catch (_: Exception) { SafeLog.receiverFailure() }
            finally { pending.finish() }
        }
    }
}
