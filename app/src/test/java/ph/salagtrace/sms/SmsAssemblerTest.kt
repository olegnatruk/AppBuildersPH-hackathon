package ph.salagtrace.sms

import org.junit.Assert.*
import org.junit.Test

class SmsAssemblerTest {
    @Test fun multipartKeepsOrderAndEarliestTime() {
        val sms = SmsAssembler.assemble(listOf(SmsPart("A", "first", 20), SmsPart("B", "other", 30), SmsPart("A", "second", 10)))
        assertEquals(2, sms.size)
        assertEquals("firstsecond", sms[0].body)
        assertEquals(10L, sms[0].timestamp)
    }
    @Test fun nullSenderIsUnknown() { assertEquals("Unknown", SmsAssembler.assemble(listOf(SmsPart(null, "hi", 1))).single().sender) }
    @Test fun duplicateMinuteIsStable() {
        assertEquals(SmsAssembler.dedupKey("A", "hi", 60_010), SmsAssembler.dedupKey("A", "hi", 119_999))
        assertNotEquals(SmsAssembler.dedupKey("A", "hi", 60_010), SmsAssembler.dedupKey("A", "hi", 120_000))
        assertNotEquals(SmsAssembler.dedupKey("A", "hi", 60_010), SmsAssembler.dedupKey("B", "hi", 60_010))
    }
    @Test fun emptyPartsAreEmpty() { assertTrue(SmsAssembler.assemble(emptyList()).isEmpty()) }
}
