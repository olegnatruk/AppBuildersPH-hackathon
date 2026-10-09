package ph.salagtrace.domain

import org.junit.Assert.*
import org.junit.Test

class MaskerTest {
    @Test fun englishOtp() = assertEquals("Your OTP is [OTP].", Masker.mask("Your OTP is 482913.").body)
    @Test fun filipinoCode() = assertEquals("Ang iyong code ay [OTP]", Masker.mask("Ang iyong code ay 482913").body)
    @Test fun standaloneSix() = assertEquals("Use [OTP]", Masker.mask("Use 123456").body)
    @Test fun fourDigitPin() = assertEquals("PIN [OTP]", Masker.mask("PIN 1234").body)
    @Test fun eightDigitVerification() = assertEquals("verification [OTP]", Masker.mask("verification 12345678").body)
    @Test fun validCard() = assertEquals("Card [CARD]", Masker.mask("Card 4111 1111 1111 1111").body)
    @Test fun invalidCardStays() = assertEquals("Card 4111 1111 1111 1112", Masker.mask("Card 4111 1111 1111 1112").body)
    @Test fun password() = assertEquals("password: [SECRET]", Masker.mask("password: hunter2").body)
    @Test fun amountStays() = assertEquals("OTP request, pay PHP 100000", Masker.mask("OTP request, pay PHP 100000").body)
    @Test fun pesoStays() = assertEquals("₱123456", Masker.mask("₱123456").body)
    @Test fun phonesStay() = assertEquals("OTP to 09171234567 or +639171234567", Masker.mask("OTP to 09171234567 or +639171234567").body)
    @Test fun accountSuffix() = assertEquals("acct: [ACCT-•••1234]", Masker.mask("acct: 12345678901234").body)
    @Test fun accountPhoneStays() = assertEquals("account 09171234567", Masker.mask("account 09171234567").body)
    @Test fun spansAfterReplacements() {
        val m = Masker.mask("OTP 123456 password: hunter2 and code 482913")
        assertEquals(3, m.spans.size)
        m.spans.forEach { assertTrue(m.body.substring(it.start, it.end).startsWith("[")) }
        assertEquals("[SECRET]", m.body.substring(m.spans[1].start, m.spans[1].end))
    }
    @Test fun cardNearPinStaysWhole() = assertEquals("PIN for card [CARD]", Masker.mask("PIN for card 4111 1111 1111 1111").body)
    @Test fun urlAndNamesStay() = assertEquals("Juan visit https://bank.example/verify", Masker.mask("Juan visit https://bank.example/verify").body)
}
