package ph.salagtrace.domain

import org.junit.Assert.*
import org.junit.Test

class VerdictFusionTest {
    private fun ai(verdict: Verdict) = AiAnalysis(verdict, Confidence.HIGH, Category.NONE, emptyList(), listOf(AiEvidence("fabricated", "")), "")
    private val none = ValidatedEvidence(emptyList(), 1)
    @Test fun missingEvidenceLowersConfidence() { assertEquals(Confidence.LOW, VerdictFusion.fuse(ai(Verdict.LIKELY_SCAM), RuleResult(30, emptyList(), emptyList()), none).confidence) }
    @Test fun highRulesSafeAiBecomesUncertain() {
        val r = VerdictFusion.fuse(ai(Verdict.LIKELY_SAFE), RuleResult(70, emptyList(), emptyList()), none)
        assertEquals(Verdict.UNCERTAIN, r.verdict); assertTrue(r.disagreement)
    }
    @Test fun unsupportedScamWithNoRulesBecomesSuspicious() {
        val r = VerdictFusion.fuse(ai(Verdict.LIKELY_SCAM), RuleResult(0, emptyList(), emptyList()), none)
        assertEquals(Verdict.SUSPICIOUS, r.verdict); assertTrue(r.disagreement)
    }
    @Test fun injectionAlwaysHasFloorAndTactic() {
        listOf(Verdict.LIKELY_SAFE, Verdict.UNCERTAIN, Verdict.SUSPICIOUS, Verdict.LIKELY_SCAM).forEach { v ->
            val r = VerdictFusion.fuse(ai(v), RuleResult(25, listOf(RuleSignal("injection_ignore", TextSpan(0, 10), Tactic.AI_MANIPULATION)), emptyList()), none)
            assertTrue(r.verdict in setOf(Verdict.SUSPICIOUS, Verdict.LIKELY_SCAM)); assertTrue(Tactic.AI_MANIPULATION in r.tactics)
        }
    }
    @Test fun highRulesDoNotUpgradeAiSuspicious() { assertEquals(Verdict.SUSPICIOUS, VerdictFusion.fuse(ai(Verdict.SUSPICIOUS), RuleResult(100, emptyList(), emptyList()), none).verdict) }
    @Test fun verifiedEvidenceIsKept() {
        val e = VerifiedEvidence("OTP", "Request", TextSpan(0, 3), Verification.VERIFIED)
        assertEquals(listOf(e), VerdictFusion.fuse(ai(Verdict.LIKELY_SCAM), RuleResult(30, emptyList(), emptyList()), ValidatedEvidence(listOf(e), 0)).evidence)
    }
}
