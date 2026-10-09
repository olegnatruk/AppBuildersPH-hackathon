package ph.salagtrace.domain

object Masker {
    private data class Replacement(val start: Int, val end: Int, val text: String, val type: MaskType)
    private val digits = Regex("(?<![\\d])\\d{4,8}(?![\\d])")
    private val otpContext = Regex("\\b(otp|one[- ]time|code|pin|mpin|verification|passcode|iyong code|kawang code)\\b", RegexOption.IGNORE_CASE)
    private val card = Regex("(?<!\\d)(?:\\d[ -]?){12,18}\\d(?!\\d)")
    private val password = Regex("\\b(?:password|pass|pw|mpin|pin)\\s*(?::|=|is\\b|ay\\b)\\s*([^\\s,;]+)", RegexOption.IGNORE_CASE)
    private val account = Regex("\\b(?:account|acct|acc\\s*no|a/c)\\s*(?:number|no\\.?)?\\s*(?::|=|is|ay)?\\s*(\\d{10,16})(?!\\d)", RegexOption.IGNORE_CASE)
    private val moneyPrefix = Regex("(?:₱|php|peso[s]?|\\bp)\\s*$", RegexOption.IGNORE_CASE)

    fun mask(body: String): MaskedMessage {
        val cards = card.findAll(body).filter { match ->
            val number = match.value.filter(Char::isDigit)
            number.length in 13..19 && luhn(number) && !isMoney(body, match.range.first)
        }.map { Replacement(it.range.first, it.range.last + 1, "[CARD]", MaskType.CARD) }.toList()
        val candidates = mutableListOf<Replacement>()
        // Reserve complete card spans so nearby "PIN" cannot mask only a card fragment.
        digits.findAll(body).forEach { m ->
            val start = m.range.first; val end = m.range.last + 1
            val context = body.substring((start - 40).coerceAtLeast(0), (end + 40).coerceAtMost(body.length))
            if ((m.value.length == 6 || otpContext.containsMatchIn(context)) && !isMoney(body, start) &&
                cards.none { start < it.end && end > it.start }) {
                candidates += Replacement(start, end, "[OTP]", MaskType.OTP)
            }
        }
        candidates += cards
        password.findAll(body).forEach { m ->
            val value = m.groups[1]!!
            candidates += Replacement(value.range.first, value.range.last + 1, "[SECRET]", MaskType.PASSWORD)
        }
        account.findAll(body).forEach { m ->
            val value = m.groups[1]!!
            if (!isPhone(value.value)) candidates += Replacement(value.range.first, value.range.last + 1, "[ACCT-•••${value.value.takeLast(4)}]", MaskType.ACCOUNT)
        }
        val selected = mutableListOf<Replacement>()
        candidates.forEach { item -> if (selected.none { item.start < it.end && item.end > it.start }) selected += item }
        val output = StringBuilder(); val spans = mutableListOf<MaskSpan>(); var cursor = 0
        selected.sortedBy { it.start }.forEach { item ->
            output.append(body, cursor, item.start)
            val start = output.length
            output.append(item.text); spans += MaskSpan(start, output.length, item.type)
            cursor = item.end
        }
        output.append(body, cursor, body.length)
        return MaskedMessage(output.toString(), spans)
    }
    private fun isMoney(body: String, start: Int): Boolean {
        val prefix = body.substring((start - 12).coerceAtLeast(0), start)
        return moneyPrefix.containsMatchIn(prefix) || (start > 0 && body[start - 1] in setOf('.', ','))
    }
    private fun isPhone(value: String) = (value.startsWith("09") && value.length == 11) || (value.startsWith("639") && value.length == 12)
    internal fun luhn(value: String): Boolean {
        if (value.all { it == '0' }) return false
        var sum = 0
        value.reversed().forEachIndexed { i, c ->
            var n = c.digitToInt()
            if (i % 2 == 1) { n *= 2; if (n > 9) n -= 9 }
            sum += n
        }
        return sum % 10 == 0
    }
}
