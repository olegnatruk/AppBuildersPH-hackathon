# Repository map

SALAG TRACE is one Android module, `:app`, application ID `ph.salagtrace`. `IMPLEMENTATION_PLAN.md` is the supplied scope; `PROJECT-BRIEF.md` summarizes the product. The implementation status and release handoff are in `IMPLEMENTATION_STATUS.md`.

## Build and runtime

`gradle/libs.versions.toml` pins libraries. AGP 8.13.1 / Gradle 8.13 / Kotlin 2.2.20 / JBR 21 build Android API 29+ with SDK 36, JVM 17 and arm64 only. `third_party/llama.cpp` is a submodule pinned to v0.6.0. `app/src/main/cpp` builds a thin JNI library in Release, including debug APKs. Native APIs are adapted to that pinned header; a HWCAP guard refuses CPUs without the optimized FP16/dot-product support. No server, account, analytics, network client, or runtime download exists.

`SalagApp.kt` / `AppContainer` manually construct Room, DataStore, the singleton engine, model manager, scheduler, notifier, and application IO scope. Startup purges expired history, scans models, and recovers queued/orphaned work. Memory pressure unloads an idle engine.

## Message path

- `sms/`: consent/monitoring-gated `goAsync` receiver, multipart grouping, minute-bucket SHA-256 deduplication. Receiver work is bounded to eight seconds.
- `domain/`: ordered secret masking, 37 rule patterns, grouped scoring, strict COD/on-arrival context with independent-risk guards, bounded untrusted-data prompt, tolerant JSON parsing, Unicode index-mapped quote validation, conservative verdict fusion, and static Fil/EN advice.
- `data/MessageRepository.kt`: masks before insert, assigns SMS/MANUAL/SAMPLE source, screens, purges retention and schedules flagged messages. Re-runs re-screen stored masked text with current rules, reset AI provenance and then request fresh actual inference.
- `work/`: unique expedited `analysis-ID` with quota fallback. Debug-only input bounds exercise real failure transitions; release uses 90 seconds / 320 tokens. A single retry handles invalid JSON. Database updates do not resurrect deleted records.
- `llm/`: private/external GGUF scan, atomic SAF import with progress, active model selection/removal; serialized engine mutex for load/decode/free, cancellation watchdog, two-minute idle unload. GBNF bounds JSON and requires evidence for flagged messages; fabricated quotes are still dropped.
- `notify/AlertNotifier.kt`: high-importance private alerts for carrier SMS, immediate high rule score and final suspicious/scam result. No SMS body, quote, link or explanation enters a notification. Public lock-screen content is generic. Tagged SMS identities cannot collide with the untagged foreground-work notification. Tap opens the matching detail. Samples/manual checks do not masquerade as incoming SMS alerts.

## Storage

Room `salag.db`, `scanned_messages` schema v1, is exported in `app/schemas/`. Unique dedupKey and receivedAt indexes support deduplication and retention. Stored fields: source/sender/time, masked text and mask spans, rule score/signals/URLs, state/verdict/confidence/category/tactics, verified evidence/explanation/language, disagreement/dropped evidence/notes, model and real load/prompt/generation/total timing, error and analysis timestamps. There is no raw SMS or raw AI JSON column. App-private storage is not SQLCipher encrypted. Manifest and extraction rules disable backup/transfer.

DataStore `salag_settings`: consent_at, onboarding_done, monitoring_enabled, ai_scope, language, retention_days, active_model, dark_theme, sms_asked, notifications_asked. Defaults: Filipino, flagged-only AI, seven-day history, dark appearance. Monitoring toggles the receiver component. Delete-all-data resets consent and disables monitoring but preserves model files.

## Interface

`MainActivity.kt` owns permissions, lifecycle refresh, and notification intents. `ui/AppViewModel.kt` exposes data/actions; Compose does not call JNI. `ui/nav/SalagNavHost.kt` routes Home, Messages, Settings, Check, and detail/id; unfinished onboarding replaces the normal navigation. Three-step onboarding includes explicit timestamped consent and denial-safe permissions. Previously refused permissions route to Android settings when appropriate.

`ui/theme/` contains Evidence Board colors, spacing, bundled IBM Plex fonts and locale configuration. Preserve the original ActivityResultRegistryOwner when providing a localized context. `res/values` and `values-tl` provide English/Filipino strings. `components/` includes verdict icon/color/text, status rows, source/state chips, dialogs and EvidenceTrace (verified amber spans, numbered markers, dotted rule borders, masked-token background). Links are plain text; copying requires a warning confirmation.

## Verification and assets

`fixtures/eval_sms.jsonl` contains 20 synthetic labeled messages; its instrumentation asset copy must stay synchronized. `fixtures/sms_suite.txt` provides ten emulator messages. `fixtures/cod_delivery.txt` is an anonymized courier regression; keep its instrumentation asset copy synchronized. `scripts/` supplies environment, checksummed model downloads, model provisioning, SMS suite, and offline permission audit. Models/APKs/build products/local.properties are ignored.

`app/src/test` exercises pure domain behavior; `app/src/androidTest` covers Room, real JNI/WorkManager/failure paths, private notification identities and Compose consent/manual/deletion flow. Benchmark, courier regression, receiver, reset and host-assisted recovery tests are explicit opt-ins; do not count assumption skips as passes. `TEST_REPORT.md` and `BENCHMARKS.md` record measured results and limitations; `phone-courier-regression.json` retains non-content metadata from the physical-phone check. `docs/licenses` and `app/src/main/assets/licenses` hold offline license texts; `docs/screenshots` contains actual emulator captures. `RELEASE_SHA256SUMS.txt` identifies the archived sideload APK. Release, privacy, setup, architecture and AI disclosures are in the root README and notices.
