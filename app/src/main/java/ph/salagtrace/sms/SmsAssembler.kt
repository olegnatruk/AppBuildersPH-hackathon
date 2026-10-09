package ph.salagtrace.sms

import java.security.MessageDigest

data class SmsPart(val sender: String?, val body: String, val timestamp: Long)
data class AssembledSms(val sender: String, val body: String, val timestamp: Long)

object SmsAssembler {
    fun assemble(parts: List<SmsPart>): List<AssembledSms> = parts.groupBy { it.sender ?: "Unknown" }.map { (sender, group) ->
        AssembledSms(sender, group.joinToString("") { it.body }, group.minOf { it.timestamp })
    }

    fun dedupKey(sender: String, body: String, timestamp: Long): String = MessageDigest.getInstance("SHA-256")
        .digest("$sender|$body|${timestamp / 60_000}".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
