package ph.salagtrace.util

import android.util.Log

/** Only fixed messages and numeric metadata are accepted; never an SMS or exception string. */
object SafeLog {
    fun received(id: Long, length: Int) { Log.i("SALAG_SMS", "received id=$id len=$length") }
    fun receiverFailure() { Log.w("SALAG_SMS", "receiver_failure") }
    fun inference(promptTokens: Int, generatedTokens: Int, latencyMs: Long) {
        Log.i("SALAG_LLM", "prompt=$promptTokens gen=$generatedTokens ms=$latencyMs")
    }
}
