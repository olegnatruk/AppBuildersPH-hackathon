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
