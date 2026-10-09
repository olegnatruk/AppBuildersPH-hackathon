package ph.salagtrace.domain

object VerdictFusion {
    fun fuse(ai: AiAnalysis, rules: RuleResult, validated: ValidatedEvidence): FinalVerdict {
        var verdict = ai.verdict; var confidence = ai.confidence; var disagreement = false
        val notes = mutableListOf<String>(); val tactics = ai.tactics.toMutableList()
        // A matching quote about COD is not evidence of deception. Keep genuine disagreement visible.
        if (RoutineDeliveryContext.matches(rules) && verdict in setOf(Verdict.LIKELY_SCAM, Verdict.SUSPICIOUS)) {
            verdict = Verdict.UNCERTAIN; confidence = Confidence.LOW; disagreement = true
            notes += "ROUTINE_DELIVERY_DISAGREEMENT"
        }
        if (validated.evidence.isEmpty() && verdict in setOf(Verdict.LIKELY_SCAM, Verdict.SUSPICIOUS)) {
            confidence = Confidence.LOW; notes += "NO_VERIFIED_EVIDENCE"
        }
        if (rules.score >= 70 && ai.verdict == Verdict.LIKELY_SAFE) { verdict = Verdict.UNCERTAIN; disagreement = true; notes += "RULE_AI_DISAGREEMENT" }
        if (rules.score < 10 && rules.signals.isEmpty() && ai.verdict == Verdict.LIKELY_SCAM && validated.evidence.isEmpty()) {
            verdict = Verdict.SUSPICIOUS; disagreement = true; notes += "RULE_AI_DISAGREEMENT"
        }
        if (rules.signals.any { it.tactic == Tactic.AI_MANIPULATION }) {
            if (verdict in setOf(Verdict.LIKELY_SAFE, Verdict.UNCERTAIN)) verdict = Verdict.SUSPICIOUS
            tactics += Tactic.AI_MANIPULATION; notes += "PROMPT_INJECTION"
        }
        return FinalVerdict(verdict, confidence, ai.category, tactics.distinct(), validated.evidence, disagreement, notes.distinct(), Provenance.ON_DEVICE_AI)
    }
}
