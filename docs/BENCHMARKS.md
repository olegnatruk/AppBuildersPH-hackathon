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

Source: [numeric smoke measurements](smoke-measurements.json). This is one synthetic sample, not a detection-accuracy estimate. The 20-fixture candidate benchmark is running; no aggregate numbers are assumed. Physical-device performance remains unmeasured.
