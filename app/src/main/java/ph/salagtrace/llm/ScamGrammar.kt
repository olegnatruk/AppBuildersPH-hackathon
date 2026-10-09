package ph.salagtrace.llm

object ScamGrammar {
    const val GBNF = """
root ::= "{" ws "\"verdict\"" ws ":" ws verdict "," ws "\"confidence\"" ws ":" ws confidence "," ws "\"category\"" ws ":" ws category "," ws "\"tactics\"" ws ":" ws tactics "," ws "\"evidence\"" ws ":" ws evidence "," ws "\"explanation\"" ws ":" ws explanation ws "}"
ws ::= [ \t\n\r]{0,4}
verdict ::= "\"likely_scam\"" | "\"suspicious\"" | "\"uncertain\"" | "\"likely_safe\""
confidence ::= "\"low\"" | "\"medium\"" | "\"high\""
category ::= "\"otp_credential_theft\"" | "\"phishing_link\"" | "\"account_suspension\"" | "\"fake_prize\"" | "\"bank_ewallet_impersonation\"" | "\"government_impersonation\"" | "\"family_impersonation\"" | "\"job_investment\"" | "\"loan_debt\"" | "\"delivery_parcel\"" | "\"other_scam\"" | "\"none\""
tactic ::= "\"urgency\"" | "\"fear_threat\"" | "\"reward_bait\"" | "\"authority_impersonation\"" | "\"credential_request\"" | "\"payment_request\"" | "\"suspicious_link\"" | "\"secrecy\"" | "\"emotional_appeal\"" | "\"ai_manipulation\""
tactics ::= "[" ws (tactic ("," ws tactic){0,3})? ws "]"
qchar ::= [^\x00-\x1F"\\]
quote ::= "\"" qchar{0,80} "\""
reason ::= "\"" qchar{0,100} "\""
explanation ::= "\"" qchar{0,320} "\""
item ::= "{" ws "\"quote\"" ws ":" ws quote "," ws "\"reason\"" ws ":" ws reason ws "}"
evidence ::= "[" ws (item ("," ws item){0,2})? ws "]"
"""
}
