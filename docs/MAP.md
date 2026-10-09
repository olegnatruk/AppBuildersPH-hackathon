# Repository map

The repository began with only `IMPLEMENTATION_PLAN.md`. There was no application, build configuration, CI, or test runner. This map is updated as those are implemented.

- `IMPLEMENTATION_PLAN.md`: product, architecture, task order, acceptance tests, and submission requirements.
- `PROJECT-BRIEF.md`: audience and scope.
- `scripts/env.sh`: local JBR 21 / Android SDK environment.
- `docs/DECISIONS.md`: architectural decisions fixed by the plan.

Current implementation status: setup. Planned paths in the plan are not yet implemented facts.

## Android build

`settings.gradle.kts` includes `:app`; `gradle/libs.versions.toml` pins dependency versions. `app/build.gradle.kts` sets package `ph.salagtrace`, Android 29 minimum / 36 compile and target, JVM 17. `MainActivity.kt` is currently a technical-spike placeholder. `app/src/main/res/xml` disables backup and transfer. The wrapper uses Gradle 8.13. App libraries match implementation plan §2.4.

## Technical spike

`app/src/main/java/ph/salagtrace/sms/SmsAssembler.kt` groups multipart PDUs in broadcast order and computes minute-bucket SHA-256 deduplication keys. `SmsReceiver.kt` is currently a content-free logging spike, not the final persisted pipeline.

`app/src/main/cpp/salag_llm.cpp` wraps pinned public llama APIs; `LlamaNative.kt` is its JNI surface. `ScamGrammar.kt` bounds JSON enum/string/array output. `NativeResult.kt` decodes real token/timing measurements. `MainActivity.kt` currently exposes model load and completion controls. `LlmSmokeTest` requires an externally provisioned model; `LlmBenchmarkTest` runs explicitly with `-e benchmark true`.

`fixtures/eval_sms.jsonl` contains 20 synthetic labeled messages (12 scam, 8 benign). The instrumentation asset is a copy; keep both synchronized. Provisioning and emulator checks live in `scripts/`.

## P0 application

- `SalagApp.kt` / `AppContainer`: manual DI for Room, DataStore, singleton engine, model manager, scheduler and receiver scope. Startup purges retention and recovers queued work. Memory pressure unloads an idle engine.
- `data/`: Room `scanned_messages` v1 (exported schema in `app/schemas/`), unique dedupKey and receivedAt index; settings in DataStore `salag_settings` (`consent_at`, `onboarding_done`, `monitoring_enabled`, `ai_scope`, `language`, `retention_days`, `active_model`).
- `sms/SmsReceiver.kt`: `goAsync` → consent/monitoring check → assemble → repository; bounded to 8 seconds, numeric-only SafeLog.
- `domain/`: masking, 37 rules, prompt, tolerant parser, Unicode index-mapped exact/partial evidence validation, fusion and static Fil/EN recommendations.
- `llm/`: private/external models folders, GGUF magic/size validation, SAF atomic import, real JNI, serial engine mutex, 120-second idle unload.
- `work/`: unique expedited `analysis-ID`, quota fallback, foreground info for Android 10/11, cancellation and crash recovery. Debug-only internal timeout/token bounds support real failure tests.
- `ui/AppViewModel.kt`: data/actions and state exposed to Compose, no JNI in screens.
- `ui/nav/SalagNavHost.kt`: temporary P0 routes `messages` and `detail/{id}`, consent, runtime permissions, import, sample input and result trace. MainActivity reads notification `messageId` extras.
- `ui/components/EvidenceTrace.kt`: verified spans / rule markers / masked tokens, with plain unclickable URLs.

`onAnalyzed` is a callback reserved for notification wiring in P1. There is no server, account, cloud API or network client.
