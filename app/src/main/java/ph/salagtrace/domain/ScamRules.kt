package ph.salagtrace.domain

data class ScamRule(val id: String, val family: String, val regex: Regex, val weight: Int, val labelFil: String, val labelEn: String, val tactic: Tactic)

object ScamRules {
    private fun r(id: String, family: String, pattern: String, weight: Int, fil: String, en: String, tactic: Tactic) =
        ScamRule(id, family, Regex(pattern, RegexOption.IGNORE_CASE), weight, fil, en, tactic)
    private const val secret = "(?:otp|\\[OTP\\]|code|pin|mpin|password|passcode)"
    val all = listOf(
        r("credential_send", "credential", "\\b(?:send|share|give|forward|provide)\\b[^.!?]{0,50}$secret", 30, "Humihingi ng OTP o lihim", "Credential request", Tactic.CREDENTIAL_REQUEST),
        r("credential_reply", "credential", "\\b(?:reply|respond|text)\\b[^.!?]{0,40}$secret", 30, "I-reply ang lihim", "Reply with a credential", Tactic.CREDENTIAL_REQUEST),
        r("credential_fil", "credential", "\\b(?:ibigay|i-?send|i-?reply|pakisend|ipadala|ishare)\\b[^.!?]{0,50}$secret", 30, "Humihingi ng OTP o lihim", "Credential request", Tactic.CREDENTIAL_REQUEST),
        r("credential_reverse", "credential", "$secret[^.!?]{0,30}\\b(?:send|reply|ibigay|ipadala)\\b", 30, "Humihingi ng OTP o lihim", "Credential request", Tactic.CREDENTIAL_REQUEST),
        r("threat_lock", "threat", "\\b(?:locked|blocked|na-?lock|mablock|ma-?bablock)\\b", 20, "Banta sa account", "Account threat", Tactic.FEAR_THREAT),
        r("threat_suspend", "threat", "\\b(?:suspended|deactivated|terminated|disabled)\\b", 20, "Banta sa account", "Account suspension", Tactic.FEAR_THREAT),
        r("threat_expire", "threat", "\\b(?:expired|expire|expiration|restricted)\\b", 20, "Pag-expire ng account", "Account expiry", Tactic.FEAR_THREAT),
        r("threat_fil", "threat", "\\b(?:i-?suspend|ma-?deactivate|isasara|isususpend)\\b", 20, "Banta sa account", "Account threat", Tactic.FEAR_THREAT),
        r("urgent_en", "urgency", "\\b(?:immediately|urgent|now|today only)\\b", 10, "Pinapamadali ka", "Urgency", Tactic.URGENCY),
        r("urgent_deadline", "urgency", "\\bwithin\\s+\\d+\\s*(?:hours?|hrs?|mins?|minutes?)\\b", 10, "Maikling palugit", "Short deadline", Tactic.URGENCY),
        r("urgent_fil", "urgency", "\\b(?:agad|ngayon din|bilisan|huling araw)\\b", 10, "Pinapamadali ka", "Urgency", Tactic.URGENCY),
        r("prize_en", "prize", "\\b(?:congratulations|congrats|winner|prize|raffle|claim your|reward|free)\\b", 20, "Pangakong premyo", "Prize or reward", Tactic.REWARD_BAIT),
        r("prize_fil", "prize", "\\b(?:nanalo|premyo|libre|pabuya)\\b", 20, "Pangakong premyo", "Prize or reward", Tactic.REWARD_BAIT),
        r("brand_bank", "brand", "\\b(?:gcash|maya|paymaya|bdo|bpi|metrobank|landbank|unionbank|security bank|rcbc)\\b", 10, "Pangalan ng bangko o wallet", "Bank or wallet name", Tactic.AUTHORITY_IMPERSONATION),
        r("brand_retail", "brand", "\\b(?:lazada|shopee|j&t|lbc|ninja ?van|dhl)\\b", 10, "Pangalan ng kumpanya", "Company name", Tactic.AUTHORITY_IMPERSONATION),
        r("brand_gov", "brand", "\\b(?:sss|philhealth|pag-?ibig|bir|lto|dict|pnp|nbi)\\b", 10, "Pangalan ng ahensya", "Government agency name", Tactic.AUTHORITY_IMPERSONATION),
        r("brand_telco", "brand", "\\b(?:globe|smart|dito)\\b", 10, "Pangalan ng telco", "Telco name", Tactic.AUTHORITY_IMPERSONATION),
        r("payment_send", "payment", "\\b(?:send money|transfer|deposit|gcash (?:to|sa))\\b", 20, "May padala o bayad", "Money or payment", Tactic.PAYMENT_REQUEST),
        r("payment_pay", "payment", "\\b(?:pay|payment|processing fee|load)\\b", 20, "May padala o bayad", "Money or payment", Tactic.PAYMENT_REQUEST),
        r("payment_fil", "payment", "\\b(?:padala|magpadala|bayad|magbayad|ipadala ang pera)\\b", 20, "May padala o bayad", "Money or payment", Tactic.PAYMENT_REQUEST),
        r("payment_amount", "payment", "(?:₱\\s*\\d|\\bphp\\s*\\d|\\bp\\d{3,})", 20, "May halaga ng pera", "Money amount", Tactic.PAYMENT_REQUEST),
        r("job_en", "job", "\\b(?:part[- ]time|work from home|daily income|commission)\\b", 15, "Alok na trabaho", "Job offer", Tactic.REWARD_BAIT),
        r("job_fil", "job", "\\b(?:kita ng|kumita|earn\\s*[₱]|sideline)\\b", 15, "Pangakong kita", "Income promise", Tactic.REWARD_BAIT),
        r("invest", "job", "\\b(?:invest|investment|double your|guaranteed return)\\b", 15, "Alok na investment", "Investment offer", Tactic.REWARD_BAIT),
        r("crypto", "job", "\\b(?:crypto|trading profit|bitcoin)\\b", 15, "Alok na crypto", "Crypto offer", Tactic.REWARD_BAIT),
        r("family_number", "family", "\\b(?:ma|pa|nay|tay|anak|mom|dad)\\b[^.!?]{0,60}(?:bagong number|new number|nawala phone)", 15, "Bagong number ng kapamilya", "Family new-number claim", Tactic.EMOTIONAL_APPEAL),
        r("family_emergency", "family", "\\b(?:ma|pa|nay|tay|anak|mom|dad)\\b[^.!?]{0,80}(?:emergency|ospital|hospital)", 15, "Emergency ng kapamilya", "Family emergency claim", Tactic.EMOTIONAL_APPEAL),
        r("delivery_en", "delivery", "\\b(?:parcel|package|delivery|customs|address)\\b", 10, "Tungkol sa delivery", "Delivery or parcel", Tactic.AUTHORITY_IMPERSONATION),
        r("delivery_fil", "delivery", "\\b(?:padala mo|ma-deliver|ipapadala ang package)\\b", 10, "Tungkol sa delivery", "Delivery or parcel", Tactic.AUTHORITY_IMPERSONATION),
        r("injection_ignore", "injection", "ignore\\s+(?:(?:all|previous)\\s+)?instructions", 25, "Sinusubukang utusan ang AI", "AI manipulation", Tactic.AI_MANIPULATION),
        r("injection_safe", "injection", "mark\\s+(?:(?:this|the)(?:\\s+message)?\\s+)?(?:as\\s+)?safe", 25, "Sinusubukang utusan ang AI", "AI manipulation", Tactic.AI_MANIPULATION),
        r("injection_role", "injection", "(?:you are an ai|system prompt|<\\|(?:im_start|system)|\\[INST\\])", 25, "Sinusubukang utusan ang AI", "AI manipulation", Tactic.AI_MANIPULATION),
        r("injection_output", "injection", "\\b(?:respond with|output only|return only)\\b", 25, "Sinusubukang utusan ang AI", "AI manipulation", Tactic.AI_MANIPULATION),
        r("secrecy", "secrecy", "\\b(?:keep (?:it|this) secret|do not tell|huwag sabihin|atin lang)\\b", 10, "Pinapalihim", "Secrecy", Tactic.SECRECY),
        r("emotional", "emotional", "\\b(?:help me|tulungan mo|nasa ospital|life or death)\\b", 10, "Pakiusap na emosyonal", "Emotional appeal", Tactic.EMOTIONAL_APPEAL),
        r("benign_en", "benign", "\\b(?:do not share|don't share|never ask for (?:your )?otp)\\b", -15, "Babala na huwag ibahagi", "Do-not-share warning", Tactic.CREDENTIAL_REQUEST),
        r("benign_fil", "benign", "\\b(?:huwag ibahagi|huwag i-?share|wag ibigay)\\b", -15, "Babala na huwag ibahagi", "Do-not-share warning", Tactic.CREDENTIAL_REQUEST),
    )
    fun label(id: String, language: AppLanguage) = all.firstOrNull { it.id == id }?.let { if (language == AppLanguage.FILIPINO) it.labelFil else it.labelEn }
        ?: when (id) { "link" -> if (language == AppLanguage.FILIPINO) "May link" else "Link present"; "suspicious_link" -> if (language == AppLanguage.FILIPINO) "Kahina-hinalang domain" else "Suspicious domain"; else -> id }
}
