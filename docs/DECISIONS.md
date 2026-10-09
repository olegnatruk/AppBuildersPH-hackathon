# Architectural decisions

The following decisions were fixed by implementation plan §3 before coding. ADR numbers are permanent.

| ADR | Decision | Reason |
|---|---|---|
| ADR-1 | Native Kotlin + Compose, single `:app` module | §2.1. One module keeps Gradle simple. |
| ADR-2 | llama.cpp submodule + custom JNI with GBNF grammar | Real local inference, guaranteed JSON, emulator-compatible. |
| ADR-3 | **No `INTERNET` permission** | Strongest verifiable privacy claim. Forces offline correctness. |
| ADR-4 | `RECEIVE_SMS` only; no `READ_SMS`, no default-SMS-app role | Minimum access. App never reads history, never blocks/deletes/replies. |
| ADR-5 | Two-stage detection: rules screen everything, LLM analyzes flagged messages | Battery and latency. Rules are always labeled "Rule check", never "AI". |
| ADR-6 | Mask before persist; LLM sees the masked text | OTPs/passwords never hit disk. Masked tokens (`[OTP]`) still carry signal for the model. |
| ADR-7 | Expedited `OneTimeWorkRequest`, unique per message, serial queue | Android-compliant background work. Receiver stays < 1 s. |
| ADR-8 | One in-process `LlmEngine` singleton, mutex-guarded, idle unload after 120 s | Avoid reloading a 1 GB model per SMS; free memory when idle. |
| ADR-9 | Deterministic recommendations per category (static Fil/EN text) | Safety advice must not hallucinate. AI produces verdict, evidence, explanation only. |
| ADR-10 | Evidence must be found in the SMS text or it is dropped | Prevents fabricated evidence from reaching the user. |
| ADR-11 | Manual DI (`AppContainer` in `SalagApp`) | No Hilt annotation-processing risk overnight. |
| ADR-12 | Native code always compiled `-DCMAKE_BUILD_TYPE=Release`, also for debug APKs | Debug-compiled llama.cpp is ~10x slower and would ruin the demo. |
