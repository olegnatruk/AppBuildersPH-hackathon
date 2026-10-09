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
