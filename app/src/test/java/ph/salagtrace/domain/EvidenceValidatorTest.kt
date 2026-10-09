package ph.salagtrace.domain

import org.junit.Assert.*
import org.junit.Test

class EvidenceValidatorTest {
    private fun ai(vararg quotes: String) = AiAnalysis(Verdict.LIKELY_SCAM, Confidence.HIGH, Category.PHISHING_LINK, emptyList(), quotes.map { AiEvidence(it, "Reason") }, "")
    @Test fun exactCaseWhitespaceOffsets() {
        val body = "Hello SEND   YOUR OTP today"
        val v = EvidenceValidator.validate(ai("send your otp"), body)
        val e = v.evidence.single()
        assertEquals("SEND   YOUR OTP", body.substring(e.span.start, e.span.end))
        assertEquals(Verification.VERIFIED, e.verification)
    }
    @Test fun curlyQuotesAndNfkcIndexMap() {
        val body = "😀 Verify “ﬁle” — now"
        val e = EvidenceValidator.validate(ai("verify \"file\" - now"), body).evidence.single()
        assertEquals("Verify “ﬁle” — now", body.substring(e.span.start, e.span.end))
    }
    @Test fun combiningCharacters() {
        val body = "Café account".replace("é", "e\u0301")
        val e = EvidenceValidator.validate(ai("café account"), body).evidence.single()
        assertEquals(body, body.substring(e.span.start, e.span.end))
    }
    @Test fun partialAtLeast70Percent() {
        val body = "Please send your password now"
        val e = EvidenceValidator.validate(ai("send your password today"), body).evidence.single()
        assertEquals(Verification.VERIFIED_PARTIAL, e.verification)
        assertEquals("send your password ", e.quote)
    }
    @Test fun fabricatedAndTinyDropped() {
        val v = EvidenceValidator.validate(ai("You won a million pesos", "hi"), "Meeting tomorrow at noon")
        assertTrue(v.evidence.isEmpty()); assertEquals(2, v.dropped)
    }
    @Test fun maskedTokenVerified() { assertEquals("[OTP]", EvidenceValidator.validate(ai("[OTP]"), "Send [OTP]").evidence.single().quote) }
    @Test fun overlappingSpansMergeForDisplay() {
        val v = EvidenceValidator.validate(ai("send your", "your password"), "send your password")
        assertEquals(listOf(TextSpan(0, 18)), EvidenceValidator.mergedSpans(v.evidence))
    }
    @Test fun lessThan70PercentDrops() { assertTrue(EvidenceValidator.validate(ai("send your password and transfer all your money"), "send your password").evidence.isEmpty()) }
}
