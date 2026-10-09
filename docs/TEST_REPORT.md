# Test report

Date: 2026-10-09, Asia/Manila. Host: Apple Silicon; Android Pixel_8 emulator, API 37 arm64, 6144672 kB RAM, 16 KB pages. These are actual checks, not the unchecked plan matrix.

| Check | Result | Evidence |
|---|---|---|
| Initial Compose APK build | Pass | Gradle 8.13 / AGP 8.13.1 / JBR 21 |
| Background SMS receiver | Pass | SALAG_SMS length 50 with app on Home |
| Multipart SMS after process kill | Pass | 348 characters, 3 PDUs assembled into one, logged length 348 |
| Assembly/deduplication JVM tests | Pass | 4 tests, testDebugUnitTest |
| Native Release build in debug APK | Pass | libsalag_llm.so packaged, 16 KB LOAD alignment 0x4000 |
| Installed network permissions | Pass | verify_offline.sh found no INTERNET / ACCESS_NETWORK_STATE |
| Real grammar-constrained Qwen 1.5B inference, network off | Pass | LlmSmokeTest raw status code 0, 24.629 s test; [measurements](smoke-measurements.json) |

The initial smoke run was skipped because the shell-owned external directory was inaccessible. That run is not counted as a pass. The directory was recreated by the app and the test rerun successfully.

Full application acceptance tests have not yet been run. Physical carrier SMS, OEM battery behavior, and physical-device speed are untested. Public publication, demo recording, and submission are not performed by this local verification.

## P0 gate — 2026-10-09 21:38 Manila

| Check | Result | Evidence |
|---|---|---|
| Full domain JVM suite | Pass | 50 tests, including fabricated/partial/Unicode evidence, masking, grammar enums, fusion, fixtures |
| Room persistence | Pass | MessageDaoTest dedup/filter/retention/delete |
| Receiver masking | Pass | ReceiverPipelineTest captured a real emulator SMS and stored [OTP], not the test code |
| WorkManager → real AI → verified evidence | Pass | AnalysisWorkerTest, actual model and worker, 11.584 s successful completion with verified spans |
| Missing model | Pass | Real worker → AI_UNAVAILABLE, modelName absent, rule result retained |
| Corrupt GGUF | Pass | Real native load rejected file; process survived |
| Timeout and cancellation | Pass | Actual native calls with a 1 ms test timeout and cancel flag |
| Worker timeout / JSON failure | Pass | Real shortened native runs → AI_TIMEOUT; one-token outputs retried once → AI_FAILED; both remain rule checks |
| Consent and permissions via UI | Pass | Reset consent/history; checked consent, saved timestamp, granted both Android dialogs, enabled monitoring |
| Automatic SMS with UI/process closed | Pass | Sender 09171238881, SMS → DONE, 13.172 s; 3 quotes matched masked text |
| Duplicate broadcast/message | Pass | Two identical sends in the same minute produced one Room row, id 11 |
| Visible investigation UI | Pass | Verdict, confidence, model/timing provenance, numbered trace, evidence list, signals, explanation, actions |
| Network disabled and permission audit | Pass | Wi-Fi/data off; verify_offline.sh passes |

The temporary P0 interface is intentionally minimal. Notifications, full onboarding/dashboard/settings, design tokens/fonts, and full language resources follow after the P0 gate. The model's Filipino prose was awkward and its explanation could infer details not stated in the SMS; only evidence quote location is validated. Confidence is model-reported, not statistically calibrated. Keep the AI fallibility notice and deterministic protective advice visible.

Two worker-test attempts failed because Qwen omitted optional evidence. Requiring a real evidence object for flagged messages fixed the trace assertion; fabricated quotes would still be dropped. The first work-testing builder attempt used its mock application context and failed the manual DI cast; the test factory now supplies the real Application. These failed attempts are not counted as passes.
