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
