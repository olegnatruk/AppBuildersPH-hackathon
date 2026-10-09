package ph.salagtrace.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val assembled = SmsAssembler.assemble(messages.map { SmsPart(it.originatingAddress, it.messageBody.orEmpty(), it.timestampMillis) })
        assembled.forEach { Log.i("SALAG_SMS", "received parts=${messages.size} len=${it.body.length} dedup=${SmsAssembler.dedupKey(it.sender, it.body, it.timestamp).take(8)}") }
    }
}
