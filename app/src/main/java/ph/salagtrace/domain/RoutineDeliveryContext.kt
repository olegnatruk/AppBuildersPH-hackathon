package ph.salagtrace.domain

/** Context, never sender authentication. Any independent risk disables this exception. */
object RoutineDeliveryContext {
    const val SIGNAL_ID = "routine_cod_delivery"
    private val cod = Regex("\\b(?:cod|cash[- ]on[- ]delivery)\\b", RegexOption.IGNORE_CASE)
    private val delivery = Regex("\\b(?:delivery|parcel|package|rider|courier)\\b", RegexOption.IGNORE_CASE)
    private val arrival = Regex("\\b(?:when|once)\\s+(?:the\\s+)?(?:rider|courier|driver)\\s+arrives?\\b|\\bupon\\s+(?:delivery|arrival)\\b|\\bon\\s+delivery\\b|\\b(?:pagdating|kapag\\s+dumating)\\s+(?:(?:ng|ang)\\s+)?(?:rider|courier)\\b", RegexOption.IGNORE_CASE)
    private val excluded = Regex("\\b(?:advance|upfront|prepay|prepaid|fees?|deposit|transfer|verify|verification|otp|pin|mpin|password|passcode|cvv|account|customs|redelivery|re-delivery|identity|identification|selfie|passport)\\b|\\[(?:OTP|CARD|SECRET|ACCT[^]]*)\\]|\\b(?:do not|don't|not|never|huwag|wag)\\b[^.!?\\n]{0,45}\\b(?:pay|cash|delivery|arriv\\w*)\\b|\\b(?:pay|payment)\\b[^.!?\\n]{0,60}\\bbefore\\b", RegexOption.IGNORE_CASE)

    internal fun signal(body: String, matches: List<Pair<ScamRule, MatchResult>>): RuleSignal? {
        if (!cod.containsMatchIn(body) || !delivery.containsMatchIn(body) || excluded.containsMatchIn(body)) return null
        val timing = arrival.find(body) ?: return null
        if (matches.any { (rule, match) ->
                when (rule.id) {
                    "brand_retail", "delivery_en", "delivery_fil", "payment_amount" -> false
                    "payment_pay" -> !match.value.equals("pay", true) && !match.value.equals("payment", true)
                    "payment_fil" -> !match.value.equals("bayad", true) && !match.value.equals("magbayad", true)
                    else -> true
                }
            }) return null
        return RuleSignal(SIGNAL_ID, TextSpan(timing.range.first, timing.range.last + 1), Tactic.AUTHORITY_IMPERSONATION)
    }

    fun matches(rules: RuleResult) = rules.score == 0 && rules.urls.isEmpty() &&
        rules.signals.size == 1 && rules.signals.single().id == SIGNAL_ID
}
