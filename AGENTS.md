# SALAG TRACE

Offline native Android SMS scam screening for Filipino, Taglish, and English. Read [PROJECT-BRIEF.md](PROJECT-BRIEF.md) for scope and [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for the authorized implementation.

| Need | Read |
|---|---|
| Files, data flow, settings | [docs/MAP.md](docs/MAP.md) |
| Coding and verification patterns | [docs/CONVENTIONS.md](docs/CONVENTIONS.md) |
| Architecture and why | [docs/DECISIONS.md](docs/DECISIONS.md) |
| Debugging and environment | [docs/GOTCHAS.md](docs/GOTCHAS.md) |
| Previous work | [docs/SESSION_LOG.md](docs/SESSION_LOG.md) |

## Hard invariants

- No INTERNET permission, cloud inference, analytics, or raw sensitive SMS persistence/logging.
- Real llama.cpp output only; rules must be labeled Rule check.
- Verify evidence against the actual masked message before highlighting it.
- Never read inbox history, block, delete, or reply to carrier SMS.
- Pin llama.cpp; do not commit GGUF files or local credentials.

## Verifying work

Source `scripts/env.sh` and run `./gradlew assembleDebug` (verified on JBR 21). Run `./gradlew testDebugUnitTest` for JVM tests. Build device tests with `./gradlew assembleDebugAndroidTest`; install both debug/test APKs and invoke `adb shell am instrument -w -r ph.salagtrace.test/androidx.test.runner.AndroidJUnitRunner` to preserve provisioned models (Gradle connected-test teardown may uninstall them). `LlmSmokeTest` needs a real model. Never count an assumption skip as a pass. Device acceptance tests are in plan §14; record actual results, including omissions.

## Keeping docs true

| Change | Update |
|---|---|
| Paths, data flow, settings, build | MAP.md |
| Code conventions / verification | CONVENTIONS.md and verification above |
| Architectural choice or API adaptation | Append DECISIONS.md ADR |
| Real debugging issue | GOTCHAS.md |
| Product scope | PROJECT-BRIEF.md |
| Every work session | Append SESSION_LOG.md |

Code wins. Keep this router thin and put each fact in one focused document. No mechanical docs checker exists.
