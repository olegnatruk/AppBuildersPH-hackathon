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

## 2026-10-09 — Final local release

Completed the installed R8 release self-test, SAF import/hash check, reboot capture, permission/monitoring/model-missing checks, private warning navigation and real injection SMS. Controlled process interruption recovered saved native work; the test runner's extra-restart timeout is retained in the gotchas. Added the optimized-CPU guard and separate tagged SMS notifications, with actual regression checks. The provisioned final suite and synthetic final-pipeline evaluation are recorded in [TEST_REPORT](TEST_REPORT.md) and [BENCHMARKS](BENCHMARKS.md); observed false positives and physical-device limits remain explicit.

Verified a fresh source checkout with the pinned public submodule and rebuilt after the final source repair. Archived the signed sideload APK and [checksum](RELEASE_SHA256SUMS.txt), captured both Filipino themes and actual AI screens, and completed README/disclosures/offline licenses/demo draft. The `v1.0.0` local checkpoint contains the deliverable. Public remote/release, human recording/post/form and unavailable physical-phone verification remain the documented handoff, not completed submissions.

## 2026-10-10 — Courier false-positive correction

A user-reported legitimate COD notice exposed generic payment/delivery scoring and a model alarm based on non-deceptive quotes. [ADR-29](DECISIONS.md) introduces guarded on-arrival context, prompt clarification and visible uncertainty for conflicting AI, with current-rule re-screening when a saved message is re-run. Tests use synthetic rider/tracking details. [Targeted real model regressions](courier-regression-results.json) passed in both languages, corrected a legacy stored screen and retained the advance-fee phishing warning. Updated unit/device results and the prompt-version limits are in [TEST_REPORT](TEST_REPORT.md). Built the signed 1.0.1 patch, updated the connected phone, and archived its checksum; the existing model remains provisioned.

The exact reported record was then re-run in the production app on the TECNO phone and became LIKELY_SAFE with real offline Qwen inference in 47.8 seconds. [Non-content metadata](phone-courier-regression.json) preserves the result without personal message details. Removed temporary UI dumps; carrier/OEM and sustained phone performance remain unverified.
