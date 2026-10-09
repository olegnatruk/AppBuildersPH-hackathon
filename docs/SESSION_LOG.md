# Session log

Append dated prose entries describing what shipped and why. Link to decisions and commits rather than duplicating their contents. Code wins; keep the other memory files accurate in the same change.

## 2026-10-09 — Bootstrap

Initialized brain-skill memory in an otherwise empty repository using the implementation plan for product intent and the installed toolchain for environment facts. [Architecture decisions](DECISIONS.md) come from the plan. App implementation and device verification follow; no benchmark numbers have been assumed.

## 2026-10-09 — Spike completed

Built the native Compose project and pinned llama.cpp v0.6.0. Background SMS capture and 348-character multipart assembly were demonstrated; four JVM assembly tests passed. Real Qwen 1.5B grammar-constrained inference passed on the API 37 / 16 KB emulator with network disabled. [Measured candidate benchmarks](BENCHMARKS.md) retain Qwen 1.5B via [ADR-16](DECISIONS.md), while documenting its false positives. Model provisioning ownership was corrected and documented in [GOTCHAS.md](GOTCHAS.md). The G1 technical conditions passed; P0 implementation follows.

## 2026-10-09 — P0 gate

Implemented the consented receiver → minimized Room → expedited WorkManager → real JNI → validated/fused result pipeline and a working message/detail UI. Fifty JVM tests and device tests for Room, capture, actual worker inference, missing/corrupt models, native timeout/cancel and worker JSON failure passed. Explicit consent and both permissions were exercised via Android UI. Two duplicate SMS sends with the process closed produced one DONE row, with actual provenance and three verified quotes. [P0 checks](TEST_REPORT.md) include honest limits on the small model's prose and accuracy. The `v0.1-p0` checkpoint precedes P1 polish.
