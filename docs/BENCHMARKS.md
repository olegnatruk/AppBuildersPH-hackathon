# On-device benchmarks

## Spike smoke — 2026-10-09

Actual Qwen2.5-1.5B-Instruct Q4_K_M on Pixel_8, API 37 arm64, 6 GB emulator RAM, 4 virtual cores, CPU inference using 2 threads. Wi-Fi and mobile data disabled. llama.cpp v0.6.0 Release build, context 1536, output cap 320.

| Measurement | Value |
|---|---|
| Load | 6131 ms |
| Prompt tokens | 183 |
| Generated tokens | 80 |
| Prefill | 5519.41 ms (33.2 tok/s) |
| Generation | 12752.7 ms (6.27 tok/s) |
| Completion | 18272.11 ms, EOS |
| Verdict | likely_scam |
| Structured JSON | Valid |

Source: [numeric smoke measurements](smoke-measurements.json). This is one synthetic sample, not a detection-accuracy estimate. The 20-fixture benchmark results are below. Physical-device performance remains unmeasured.

## Candidate benchmark — 2026-10-09

All 20 synthetic fixtures were forced through AI to measure each model, including benign messages that production rules may skip. Same spike prompt, grammar, 320-token cap, temperature 0.2, seed 42, two threads, network disabled. This measures the spike prompt, not final pipeline accuracy.

| Model | Load ms | p50 / max ms | Valid JSON | Correct verdicts | Exact evidence match | Peak RSS MiB | Prefill / generation tok/s |
|---|---:|---:|---:|---:|---:|---:|---:|
| qwen2.5-1.5b-instruct-q4_k_m.gguf | 4477 | 20076 / 30047 | 20/20 | 13/20 | 0/0 | 2200.0 | 36.2 / 6.6 |
| qwen2.5-0.5b-instruct-q4_k_m.gguf | 2939 | 14628 / 23121 | 20/20 | 9/20 | 0/0 | 892.0 | 61.4 / 12.7 |
| gemma-3-1b-it-Q4_K_M.gguf | 4209 | 8409 / 46652 | 20/20 | 11/20 | 35/36 | 1642.7 | 73.5 / 15.5 |

**Selection: Qwen2.5-1.5B-Instruct Q4_K_M.** All candidates passed JSON ≥95% and p50 ≤60 s. Qwen 1.5B had the highest accuracy; its two-message lead over Gemma exceeds the ≤1-message tie threshold. Keep 0.5B available for lower RAM. The benchmark finished within the 45-minute limit.

**Limit:** the spike prompt produced substantial false positives (Qwen 1.5B got seven of eight benign messages wrong). Do not claim broad accuracy from this small synthetic set. Production uses the full prompt with rule context, masking, selective AI, verified evidence, and fusion; these need their own end-to-end checks. Gemma also missed the injection fixture, motivating the deterministic manipulation floor in the planned fusion. No physical-device benchmarks exist.

Source: [all numeric per-fixture measurements](benchmark-measurements.json). The evidence metric is exact normalized quote matching in the spike harness, before the production partial-match validator exists. Cold/warm performance varied sharply; medians include all runs.

## Final production pipeline — 2026-10-09

After full prompt examples, masking, FLAGGED gating, quote verification, fusion and OS-stop recovery, `FinalPipelineBenchmarkTest` completed all 20 fixtures in 274.832 seconds on the same offline emulator. Explanations were requested in Filipino. Temperature/seed/context/output limit remain 0.2/42/1536/320. Two terminal fixtures ran while the host performed a release build, so max latency includes host contention.

| Measurement | Actual result |
|---|---:|
| Binary flagged/not-flagged decisions matching labels | 19/20 |
| Scam fixtures flagged | 12/12 |
| Benign fixtures not flagged | 7/8 |
| Actual AI completions / rule-only records | 14 / 6 |
| Successful structured AI results | 14/14 |
| AI latency p50 / max | 14902 / 53942 ms |
| Retained verified quotes / dropped quotes | 30 / 0 |
| AI unavailable/timeout/failure | 0 |

`receipt` was falsely labeled likely_scam. The binary metric treats LIKELY_SCAM/SUSPICIOUS as flagged and UNCERTAIN/LIKELY_SAFE as not flagged; it does not assess category, prose quality, calibrated confidence, or the truth of reasoning. Verification covers quote location only. These are the same 20 synthetic fixtures used while developing the prompt, not an independent held-out test. Do not compare the 95% combined rules/AI result with the spike's forced-AI score as a measured generalization gain. The model still infers unstated details in some messages. Physical devices and real-world scam accuracy remain untested.

Source: [final numeric per-fixture results](final-pipeline-measurements.json). A prior exploratory run exposed a JobScheduler interruption bug, was aborted, and is excluded from this completed measurement. `StoppedWorkerTest` separately passed the repaired queue/explicit-cancel regression with real native inference.
