package ph.salagtrace.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface ParseOutcome {
    data class Success(val analysis: AiAnalysis) : ParseOutcome
    data class Failure(val reason: String = "AI returned invalid structured output") : ParseOutcome
}
@Serializable private data class AiRaw(
    val verdict: String, val confidence: String, val category: String,
    val tactics: List<String> = emptyList(), val evidence: List<EvidenceRaw> = emptyList(), val explanation: String,
)
@Serializable private data class EvidenceRaw(val quote: String, val reason: String)
object ResponseParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    fun parse(text: String): ParseOutcome = try {
        val first = text.indexOf('{'); val last = text.lastIndexOf('}')
        if (first < 0 || last < first) ParseOutcome.Failure() else {
            val raw = json.decodeFromString<AiRaw>(text.substring(first, last + 1))
            val verdict = Verdict.entries.firstOrNull { it.wire == raw.verdict } ?: Verdict.UNCERTAIN
            val confidence = Confidence.entries.firstOrNull { it.wire == raw.confidence } ?: Confidence.LOW
            val category = Category.entries.firstOrNull { it.wire == raw.category } ?: Category.OTHER_SCAM
            ParseOutcome.Success(AiAnalysis(verdict, confidence, category,
                raw.tactics.mapNotNull { v -> Tactic.entries.firstOrNull { it.wire == v } }.distinct().take(4),
                raw.evidence.take(3).map { AiEvidence(clean(it.quote, 80), clean(it.reason, 100)) }, clean(raw.explanation, 320)))
        }
    } catch (_: Exception) { ParseOutcome.Failure() }
    private fun clean(s: String, length: Int) = s.filterNot(Char::isISOControl).take(length).let { if (it.lastOrNull()?.isHighSurrogate() == true) it.dropLast(1) else it }
}
