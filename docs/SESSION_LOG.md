# Session log

Append dated prose entries describing what shipped and why. Link to decisions and commits rather than duplicating their contents. Code wins; keep the other memory files accurate in the same change.

## 2026-10-09 — Bootstrap

Initialized brain-skill memory in an otherwise empty repository using the implementation plan for product intent and the installed toolchain for environment facts. [Architecture decisions](DECISIONS.md) come from the plan. App implementation and device verification follow; no benchmark numbers have been assumed.

## 2026-10-09 — Spike completed

Built the native Compose project and pinned llama.cpp v0.6.0. Background SMS capture and 348-character multipart assembly were demonstrated; four JVM assembly tests passed. Real Qwen 1.5B grammar-constrained inference passed on the API 37 / 16 KB emulator with network disabled. [Measured candidate benchmarks](BENCHMARKS.md) retain Qwen 1.5B via [ADR-16](DECISIONS.md), while documenting its false positives. Model provisioning ownership was corrected and documented in [GOTCHAS.md](GOTCHAS.md). The G1 technical conditions passed; P0 implementation follows.

## 2026-10-09 — P0 gate

Implemented the consented receiver → minimized Room → expedited WorkManager → real JNI → validated/fused result pipeline and a working message/detail UI. Fifty JVM tests and device tests for Room, capture, actual worker inference, missing/corrupt models, native timeout/cancel and worker JSON failure passed. Explicit consent and both permissions were exercised via Android UI. Two duplicate SMS sends with the process closed produced one DONE row, with actual provenance and three verified quotes. [P0 checks](TEST_REPORT.md) include honest limits on the small model's prose and accuracy. The `v0.1-p0` checkpoint precedes P1 polish.

## P1 screens and release hardening — 2026-10-09

Implemented bundled IBM Plex fonts, accessible dark/light Evidence Board themes, English/Filipino resource catalogs, three-step timestamped consent and denial-safe permission flow, Home status/counts/recent results, history filters/deletion confirmations, masked manual checker, investigation quote/rule trace, copy-link warning, model picker/SAF progress/self-test, privacy/limits/about, and private SMS-only deep-linked alerts. Re-run clears stale AI category/tactics. Added a phishing prompt example after observed OTP-category bias.

Verification exposed two issues and repaired them: localized Context hid the ActivityResultRegistryOwner; the older transitive Espresso called a removed API on Android 37. Compose testing then required scrolling to a lazy-list item and selecting the quote rather than its legend. Actual provisioned device suite: 8 pass, 4 optional skips; 50 JVM tests pass. Release build with R8 succeeds; lint errors (configuration reads/telephony feature) repaired, no baseline suppression. Remaining lint warnings mainly concern intentionally pinned versions, arm64-only support, and style suggestions. Publication, physical-phone testing, and human video/form tasks remain separate.

Background verification found a queued-worker cancellation bug under JobScheduler preemption. Repaired OS-stop recovery without changing the native timeout, and added a real queued-worker regression test (passed, 15.306 s). Foreground and background ten-SMS suites each captured 10/10; all eight flagged messages in each suite completed real AI, two benign messages remained rule-only. A 273-character Filipino multipart message also completed. Force-stop correctly suppressed a new broadcast. Final pipeline evaluation was restarted after the fix.
