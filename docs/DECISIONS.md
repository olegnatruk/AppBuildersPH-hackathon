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

## ADR-13 — Reproduce the Compose template with a verified toolchain

The plan permits replicating the Empty Activity template. Use AGP 8.13.1, Gradle 8.13, Kotlin 2.2.20 and KSP 2.2.20-2.0.4, rather than driving Android Studio's wizard. This retains the single-module Compose build and avoids AGP 9's different Kotlin integration. Compatibility was checked against [AGP release notes](https://developer.android.com/build/releases/agp-8-13-0-release-notes) and [KSP's release](https://github.com/google/ksp/releases/tag/2.2.20-2.0.4).

## ADR-14 — Pin llama.cpp v0.6.0

Pinned [llama.cpp v0.6.0](https://github.com/ggml-org/llama.cpp/releases/tag/v0.6.0), commit `d81235049384534c167caea52b85a694f6103d14`, in `third_party/llama.cpp`. The newest release at setup uses a semantic tag rather than the plan's `bNNNN` naming. Public JNI APIs are checked against this pinned header. No runtime update mechanism exists.

## ADR-15 — Native options and UTF-8 adaptation

v0.6.0 replaced the plan's LLAMA_CURL option with LLAMA_OPENSSL. Disable LLAMA_OPENSSL, LLAMA_BUILD_APP, and LLAMA_BUILD_COMMON alongside tests/tools/server so no unnecessary targets or downloads are built. NDK r28 and CMake 3.22.1 are used first. JNI converts UTF-16 to real UTF-8 instead of modified UTF-8, preserving Filipino text and emoji. `llama_model_params.use_mmap` was replaced by `load_mode`; use `LLAMA_LOAD_MODE_MMAP`. Native logs are suppressed; only Kotlin measurement metadata may be logged.

## ADR-16 — Retain Qwen 1.5B after measured selection

The spike benchmark compared all three planned models on 20 fixtures. Qwen 1.5B met valid-JSON and latency gates and had the highest verdict accuracy (13/20, versus Gemma 11/20 and Qwen 0.5B 9/20). The two-message lead is larger than the plan's tie window. Retain the default. These limited spike results expose false positives and are not a production accuracy claim; see `BENCHMARKS.md`.

## ADR-17 — Reserve full card spans during ordered masking

Keep the plan's OTP → card → secret → account replacement priority, but identify valid full card spans first and exclude those from OTP candidates. Otherwise a nearby PIN/code label could redact only four digits of a spaced card and leave the remainder on disk. Currency amounts and phone numbers remain visible; tests cover these overlaps and final masked-coordinate offsets.

## ADR-18 — Family scoring and recovery metadata

Rule scores count each family once (maximum matched weight), preventing repeated synonyms or repeated SMS text from inflating risk. Brand mentions add a combined boost only with another signal. Benign do-not-share warnings are excluded from credential-request gating, and do not suppress positive requests or links. Add `analysisStartedAt` and native prefill/generation/load timing columns to Room version 1 for honest recovery and self-test metrics; no raw body or raw AI JSON column exists.

## ADR-19 — Calibrate the full prompt after spike false positives

Keep the plan's untrusted-data, exact-quote, concise-language instructions and add explicit benign examples (ordinary chats, receipts, delivery updates) and the distinction between mentioning an OTP/payment and requesting one. This responds to measured spike false positives without changing model selection or inventing accuracy claims. Escape chat role tokens as well as SMS closing tags; truncate without splitting an emoji surrogate pair. Native output is still grammar constrained, validated, and fused. Static recommendations use generic official-channel wording instead of unverified hotline numbers; [GCash's official guidance](https://help.gcash.com/hc/en-us/articles/19578518834457-How-can-I-protect-my-GCash-account) supports the credential and verification advice.

## ADR-20 — Require an evidence object for flagged nonempty messages

Qwen ignored optional evidence even with prompt examples, so flagged-message completions use the same bounded grammar with a minimum of one evidence object (still within the schema's 0–3 range). Unflagged ALL-scope completions retain optional evidence. This requests a real model quote; it does not manufacture one. Every quote still passes the same exact/partial validator and can be dropped, with confidence reduced. The user's task instruction repeats the requested language directly before the untrusted fields to help this small model follow it.

## ADR-21 — Real failure-state instrumentation with debug bounds

Debug workers may accept internal test input for shorter timeouts and generation caps. Release workers always use the planned 90 seconds and 320 tokens. This lets instrumented tests exercise real native timeout and truncated JSON → retry → AI_FAILED transitions, rather than returning fabricated inference. The debug knobs are not exported to a UI, receiver, or external intent.

## ADR-22 — Accessible light accent and source-aware private alerts

The specified light amber #B7791F has only 3.64:1 contrast on white, below the same plan's 4.5:1 requirement. Use #946019 (5.32:1) for light theme primary text/buttons; retain the dark amber. Darken light suspicious text to #A94708 so its tinted badge also meets contrast. Verdicts always include an icon and words. Only Source.SMS produces alerts; samples and pasted messages stay in-app. Notification body contains a masked phone sender plus provenance, never SMS content, quotes, links, or AI prose. A generic public version protects lock screens.

## ADR-23 — Preserve permission owner under locale overrides

`createConfigurationContext` no longer exposes the ActivityResultRegistryOwner through its context chain. Capture and provide the original owner before swapping LocalContext, so runtime permission and SAF launchers work with Filipino/English strings. An actual emulator launch exposed this and the Compose flow test verifies the corrected startup and consent path.

## ADR-24 — Add a phishing example without inventing AI explanations

The full prompt's OTP example biased a link-only sample toward OTP theft. Add an exact phishing quote/example and explicitly prohibit that category without an OTP/password request. AI prose and category may still be wrong; quote verification establishes only correspondence to message text. Preserve model-generated output with a fallibility label and static advice; do not replace it with fabricated AI conclusions.

## ADR-25 — Pin Espresso 3.7.0 for API 37 device tests

Compose's transitive older Espresso failed before the UI test could interact: reflective `InputManager.getInstance` is unavailable on API 37. Add a test-only explicit Espresso 3.7.0 dependency. Its [official release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0) document using getSystemService instead. No production dependency or permission changes.

## ADR-26 — OS-stopped work remains queued

A real background SMS burst caused JobScheduler to stop a worker waiting for the engine, then reschedule it. Treating every coroutine cancellation as AI_FAILED made the replacement worker skip that message. Cancellation from Android now conditionally resets only QUEUED_AI/ANALYZING rows to QUEUED_AI; WorkManager handles rescheduling. The UI marks explicit cancellation terminal before stopping work, so it cannot be re-queued. Deleted, completed or explicitly canceled rows are not changed. Evaluation wait bounds allow queued burst work without changing the native 90-second inference cap.

## ADR-27 — Refuse unsupported optimized CPUs safely

The plan's ARMv8.2 + dotprod backend cannot run on every arm64 Android 10 device. Check kernel HWCAP for scalar/vector FP16 and dot-product support before llama backend initialization. Unsupported hardware gets model-load failure and retains rule checks rather than entering instructions it cannot execute. The emulator's supported path is tested; an older physical CPU is not available for validation. Expose CPU requirements in setup/limits docs.

## ADR-28 — Tag SMS alerts independently of foreground work

Android identifies a notification by tag and ID. Give SMS alerts the stable tag `sms-<row id>` so the first message's numeric ID 1001 cannot replace WorkManager's untagged foreground notification on Android 10/11. Final AI updates reuse the same alert identity; deleting a row cancels that tagged alert. NotificationPrivacyTest verifies coexistence, cancellation, masked sender, content omission and private/public versions.
