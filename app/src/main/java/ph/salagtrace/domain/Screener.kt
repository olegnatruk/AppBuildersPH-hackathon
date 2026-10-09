package ph.salagtrace.domain

import java.net.URI

object Screener {
    private val urls = Regex("(?:https?://|www\\.)[^\\s<>]+|\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,24}(?:/[^\\s<>]*)?", RegexOption.IGNORE_CASE)
    private val riskyTlds = setOf("xyz", "top", "icu", "click", "live", "shop", "vip", "cc", "info", "online")
    private val shorteners = setOf("bit.ly", "tinyurl.com", "t.co", "cutt.ly", "is.gd", "rb.gy", "s.id")
    private val official = mapOf("gcash" to "gcash.com", "maya" to "maya.ph", "bdo" to "bdo.com.ph", "bpi" to "bpi.com.ph", "shopee" to "shopee.ph", "lazada" to "lazada.com.ph")
    private val negation = Regex("(?:do not|don't|never|huwag|wag)\\s+(?:ever\\s+)?$", RegexOption.IGNORE_CASE)

    fun screen(body: String, sender: String = ""): RuleResult {
        val links = urls.findAll(body).map { match ->
            val url = match.value.trimEnd('.', ',', '!', '?', ')', ']', ';', ':', '"', '\'')
            val uri = runCatching { URI(if (url.startsWith("http", true)) url else "https://$url") }.getOrNull()
            val domain = uri?.host?.lowercase() ?: url.substringBefore('/').lowercase()
            val lookalike = official.any { (brand, real) -> brand in domain && domain != real && !domain.endsWith(".$real") }
            UrlInfo(url, domain, TextSpan(match.range.first, match.range.first + url.length), domain in shorteners || domain.substringAfterLast('.') in riskyTlds || lookalike)
        }.distinctBy { it.url }.toList()
        val signals = mutableListOf<RuleSignal>()
        val families = mutableMapOf<String, Int>()
        val matches = ScamRules.all.filter { it.family != "benign" }.flatMap { rule ->
            rule.regex.findAll(body).filter { m ->
                rule.family != "credential" || !negation.containsMatchIn(body.substring((m.range.first - 24).coerceAtLeast(0), m.range.first))
            }.map { rule to it }.toList()
        }
        if (links.isEmpty()) RoutineDeliveryContext.signal(body, matches)?.let {
            return RuleResult(0, listOf(it), emptyList())
        }
        val hasRequest = matches.any { it.first.family == "credential" }
        matches.forEach { (rule, m) ->
            signals += RuleSignal(rule.id, TextSpan(m.range.first, m.range.last + 1), rule.tactic)
            if (rule.family != "brand" || matches.any { it.first.family != "brand" }) families[rule.family] = maxOf(families[rule.family] ?: 0, rule.weight)
        }
        if (links.isEmpty() && !hasRequest) {
            ScamRules.all.filter { it.family == "benign" }.forEach { rule ->
                rule.regex.findAll(body).forEach { m ->
                    // A benign warning is context, not a credential-request signal.
                    signals += RuleSignal(rule.id, TextSpan(m.range.first, m.range.last + 1), Tactic.AUTHORITY_IMPERSONATION)
                    families["benign"] = -15
                }
            }
        }
        links.forEach { link ->
            signals += RuleSignal("link", link.span, Tactic.SUSPICIOUS_LINK)
            families["link"] = 15
            if (link.suspicious) { signals += RuleSignal("suspicious_link", link.span, Tactic.SUSPICIOUS_LINK); families["risky_link"] = 15 }
        }
        return RuleResult(families.values.sum().coerceIn(0, 100), signals.distinctBy { it.id to it.span }, links)
    }
    fun ruleVerdict(rules: RuleResult): Verdict = when {
        RoutineDeliveryContext.matches(rules) -> Verdict.LIKELY_SAFE
        rules.score >= 70 -> Verdict.LIKELY_SCAM
        rules.signals.any { it.tactic == Tactic.AI_MANIPULATION } -> Verdict.SUSPICIOUS
        rules.score >= 25 -> Verdict.SUSPICIOUS
        rules.score < 10 && rules.signals.isEmpty() -> Verdict.LIKELY_SAFE
        else -> Verdict.UNCERTAIN
    }
}
