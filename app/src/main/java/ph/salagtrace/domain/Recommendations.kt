package ph.salagtrace.domain

object Recommendations {
    fun forCategory(category: Category, language: AppLanguage): List<String> {
        val fil = language == AppLanguage.FILIPINO
        val generic = if (fil) listOf("Huwag magmadali. I-verify gamit ang opisyal na app o hotline ng kumpanya.", "Huwag buksan ang link o magpadala ng pera dahil lang sa text.", "Kung may naibigay ka nang lihim o pera, kontakin agad ang bangko o wallet sa opisyal na app.")
            else listOf("Pause and verify through the company's official app or hotline.", "Avoid opening links or sending money based only on a text.", "If you already shared a secret or sent money, contact your bank or wallet through its official app.")
        val specific = when (category) {
            Category.OTP_CREDENTIAL_THEFT -> if (fil) "Huwag ibigay ang OTP, MPIN, PIN o password kahit kanino." else "Never share an OTP, MPIN, PIN, or password."
            Category.PHISHING_LINK, Category.ACCOUNT_SUSPENSION, Category.BANK_EWALLET_IMPERSONATION -> if (fil) "Buksan mismo ang opisyal na app; tawagan ang bangko gamit ang numero sa likod ng card." else "Open the official app directly; call your bank using the number on your card."
            Category.FAMILY_IMPERSONATION -> if (fil) "Tawagan ang kapamilya sa dati mong alam na number bago magpadala." else "Call your family member on a number you already know before sending money."
            Category.FAKE_PRIZE -> if (fil) "I-verify ang promo sa opisyal na app. Huwag magbayad para makuha ang sinasabing premyo." else "Verify the promotion in the official app. Avoid paying to claim an alleged prize."
            Category.JOB_INVESTMENT, Category.LOAN_DEBT -> if (fil) "I-verify ang alok at kumpanya sa mga opisyal na channel bago magbayad o mag-invest." else "Verify the offer and company through official channels before paying or investing."
            Category.GOVERNMENT_IMPERSONATION -> if (fil) "Kontakin mismo ang ahensya gamit ang opisyal nitong website o opisina." else "Contact the agency directly through its official website or office."
            Category.DELIVERY_PARCEL -> if (fil) "Tingnan ang tracking sa opisyal na app ng courier; i-verify ang sinasabing bayad." else "Check tracking in the courier's official app and verify any claimed fee."
            else -> null
        }
        return listOfNotNull(specific) + generic
    }
}
