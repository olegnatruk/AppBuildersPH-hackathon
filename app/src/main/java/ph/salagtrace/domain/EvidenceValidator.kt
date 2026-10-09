package ph.salagtrace.domain

import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale

object EvidenceValidator {
    private data class Normalized(val text: String, val starts: List<Int>, val ends: List<Int>)
    fun validate(analysis: AiAnalysis, body: String): ValidatedEvidence {
        val normalized = normalize(body)
        val verified = mutableListOf<VerifiedEvidence>(); var dropped = 0
        analysis.evidence.forEach { item ->
            val quote = normalize(item.quote).text
            if (quote.length < 3) { dropped++; return@forEach }
            val exact = normalized.text.indexOf(quote)
            var start = exact; var length = quote.length; var status = Verification.VERIFIED
            if (exact < 0) {
                val partial = commonSubstring(quote, normalized.text)
                start = partial.first; length = partial.second; status = Verification.VERIFIED_PARTIAL
                if (length < 12 || length < quote.length * 0.7) { dropped++; return@forEach }
            }
            val span = TextSpan(normalized.starts[start], normalized.ends[start + length - 1])
            verified += VerifiedEvidence(body.substring(span.start, span.end), item.reason, span, status)
        }
        return ValidatedEvidence(verified, dropped)
    }
    fun mergedSpans(items: List<VerifiedEvidence>): List<TextSpan> {
        val merged = mutableListOf<TextSpan>()
        items.map { it.span }.sortedBy { it.start }.forEach { s ->
            val previous = merged.lastOrNull()
            if (previous != null && s.start < previous.end) merged[merged.lastIndex] = TextSpan(previous.start, maxOf(previous.end, s.end))
            else merged += s
        }
        return merged
    }
    private fun normalize(original: String): Normalized {
        val text = StringBuilder(); val starts = mutableListOf<Int>(); val ends = mutableListOf<Int>()
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT); iterator.setText(original)
        var start = iterator.first(); var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val normalized = Normalizer.normalize(original.substring(start, end), Normalizer.Form.NFKC).lowercase(Locale.ROOT)
            normalized.forEach { raw ->
                val c = when (raw) { '‘', '’' -> '\''; '“', '”' -> '"'; '‐', '‑', '–', '—', '−' -> '-'; else -> raw }
                if (c.isWhitespace()) {
                    if (text.isNotEmpty() && text.last() == ' ') ends[ends.lastIndex] = end
                    else if (text.isNotEmpty()) { text.append(' '); starts += start; ends += end }
                } else { text.append(c); starts += start; ends += end }
            }
            start = end; end = iterator.next()
        }
        if (text.lastOrNull() == ' ') { text.setLength(text.length - 1); starts.removeAt(starts.lastIndex); ends.removeAt(ends.lastIndex) }
        return Normalized(text.toString(), starts, ends)
    }
    private fun commonSubstring(quote: String, body: String): Pair<Int, Int> {
        var previous = IntArray(body.length + 1); var bestLength = 0; var bestEnd = 0
        for (q in quote) {
            val current = IntArray(body.length + 1)
            body.forEachIndexed { index, b ->
                if (q == b) { current[index + 1] = previous[index] + 1; if (current[index + 1] > bestLength) { bestLength = current[index + 1]; bestEnd = index + 1 } }
            }
            previous = current
        }
        return (bestEnd - bestLength) to bestLength
    }
}
