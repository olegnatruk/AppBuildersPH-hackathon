package ph.salagtrace.domain

import kotlinx.serialization.Serializable

@Serializable enum class Verdict { LIKELY_SCAM, SUSPICIOUS, UNCERTAIN, LIKELY_SAFE; val wire get() = name.lowercase() }
@Serializable enum class Confidence { LOW, MEDIUM, HIGH; val wire get() = name.lowercase() }
@Serializable enum class Category {
    OTP_CREDENTIAL_THEFT, PHISHING_LINK, ACCOUNT_SUSPENSION, FAKE_PRIZE,
    BANK_EWALLET_IMPERSONATION, GOVERNMENT_IMPERSONATION, FAMILY_IMPERSONATION,
    JOB_INVESTMENT, LOAN_DEBT, DELIVERY_PARCEL, OTHER_SCAM, NONE;
    val wire get() = name.lowercase()
}
@Serializable enum class Tactic {
    URGENCY, FEAR_THREAT, REWARD_BAIT, AUTHORITY_IMPERSONATION, CREDENTIAL_REQUEST,
    PAYMENT_REQUEST, SUSPICIOUS_LINK, SECRECY, EMOTIONAL_APPEAL, AI_MANIPULATION;
    val wire get() = name.lowercase()
}
@Serializable enum class AnalysisState { SCREENED, QUEUED_AI, ANALYZING, DONE, AI_UNAVAILABLE, AI_TIMEOUT, AI_FAILED }
@Serializable enum class Source { SMS, MANUAL, SAMPLE }
enum class AiScope { FLAGGED, ALL }
enum class AppLanguage { FILIPINO, ENGLISH }
@Serializable enum class MaskType { OTP, CARD, PASSWORD, ACCOUNT }
@Serializable enum class Verification { VERIFIED, VERIFIED_PARTIAL }
enum class Provenance { RULE_CHECK, ON_DEVICE_AI }
@Serializable data class TextSpan(val start: Int, val end: Int)
@Serializable data class MaskSpan(val start: Int, val end: Int, val type: MaskType)
data class MaskedMessage(val body: String, val spans: List<MaskSpan>)
@Serializable data class RuleSignal(val id: String, val span: TextSpan, val tactic: Tactic)
@Serializable data class UrlInfo(val url: String, val domain: String, val span: TextSpan, val suspicious: Boolean)
@Serializable data class RuleResult(val score: Int, val signals: List<RuleSignal>, val urls: List<UrlInfo>) {
    fun needsAi(scope: AiScope) = scope == AiScope.ALL || score >= 25 || urls.isNotEmpty() ||
        signals.any { it.tactic in setOf(Tactic.CREDENTIAL_REQUEST, Tactic.PAYMENT_REQUEST, Tactic.AI_MANIPULATION) }
}
data class AiEvidence(val quote: String, val reason: String)
data class AiAnalysis(val verdict: Verdict, val confidence: Confidence, val category: Category, val tactics: List<Tactic>, val evidence: List<AiEvidence>, val explanation: String)
@Serializable data class VerifiedEvidence(val quote: String, val reason: String, val span: TextSpan, val verification: Verification)
data class ValidatedEvidence(val evidence: List<VerifiedEvidence>, val dropped: Int)
data class FinalVerdict(
    val verdict: Verdict, val confidence: Confidence, val category: Category,
    val tactics: List<Tactic>, val evidence: List<VerifiedEvidence>, val disagreement: Boolean,
    val notes: List<String>, val provenance: Provenance,
)
