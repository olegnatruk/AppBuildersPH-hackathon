package ph.salagtrace.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import ph.salagtrace.llm.ScamGrammar

class GrammarConsistencyTest {
    @Test fun everyOutputEnumIsInGrammar() {
        (Verdict.entries.map { it.wire } + Confidence.entries.map { it.wire } + Category.entries.map { it.wire } + Tactic.entries.map { it.wire })
            .forEach { assertTrue("Grammar missing $it", ScamGrammar.GBNF.contains("\\\"$it\\\"")) }
    }
}
