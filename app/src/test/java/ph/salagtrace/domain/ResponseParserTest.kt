package ph.salagtrace.domain

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ResponseParserTest {
    private fun json(verdict: String = "likely_scam", explanation: String = "Be careful") = buildJsonObject {
        put("verdict", verdict); put("confidence", "high"); put("category", "phishing_link")
        put("tactics", buildJsonArray { add("urgency"); add("nonsense") })
        put("evidence", buildJsonArray { add(buildJsonObject { put("quote", "Click here"); put("reason", "Link") }) })
        put("explanation", explanation); put("extra", "ignored")
    }.toString()
    @Test fun validAndWrapped() {
        val p = ResponseParser.parse("Here is the JSON:\n${json()}\nDone") as ParseOutcome.Success
        assertEquals(Verdict.LIKELY_SCAM, p.analysis.verdict)
        assertEquals(listOf(Tactic.URGENCY), p.analysis.tactics)
    }
    @Test fun unknownEnumBecomesUncertain() { assertEquals(Verdict.UNCERTAIN, (ResponseParser.parse(json("nonsense")) as ParseOutcome.Success).analysis.verdict) }
    @Test fun limitsAndControlCharacters() { assertEquals(320, (ResponseParser.parse(json(explanation = "x".repeat(400) + "\u0001")) as ParseOutcome.Success).analysis.explanation.length) }
    @Test fun failuresAreSafe() {
        listOf("", "garbage", "{}", "{not json}").forEach { assertTrue(ResponseParser.parse(it) is ParseOutcome.Failure) }
    }
}
