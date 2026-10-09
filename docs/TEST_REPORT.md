# Test report

Date: 2026-10-09, Asia/Manila. Actual checks on an Apple Silicon host and Pixel_8 API 37 arm64 emulator: 6 GB guest RAM, four virtual cores, 16 KB pages. Wi-Fi/mobile data disabled for inference. No physical Android phone is connected. A passing emulator test is not a physical carrier/OEM reliability claim.

## v1.0.0 build and automated checks

| Check | Observed result |
|---|---|
| JVM domain suite | **50 passed**, zero failures/errors/skips: masking, multipart/dedup, rule screening, JSON/enums, prompt escaping, Unicode/exact/partial/fabricated evidence and conservative fusion |
| Provisioned default device suite | **10 passed, 6 explicit optional skips**, zero failures, 84.308 s; [individual results](device-test-results.json) |
| Real JNI and WorkManager | Successful actual inference, valid grammar-constrained JSON, recorded model/tokens/time, verified quote spans |
| Native/worker failure paths | Missing/corrupt GGUF, real timeout/cancel, truncated JSON with one retry; retained Rule check results, no fabricated AI |
| Queue preemption | StoppedWorkerTest: a real worker queued behind native inference can resume; explicit cancellation stays terminal |
| Room | Persistence, dedup, filters, retention, delete-record/delete-all assertions |
| Compose | Explicit consent gate, finish with denied permissions, masked MANUAL record, actual delete confirmation |
| Notification privacy/identity | Masked sender and provenance only, private visibility/generic public version, PendingIntent, coexistence with untagged foreground ID 1001, targeted cancellation |
| Debug/test/release build and release lint | Pass; no lint errors. Remaining warnings include pinned dependencies, intentional arm64-only support and style suggestions |
| Release package | R8, version 1.0.0, 10,100,255 bytes; debug-key signed for sideload; apksigner and zipalign `-P 16` pass |
| Native alignment | ELF LOAD alignment 0x4000; actual execution on a 16 KB emulator |
| Network audit | Installed release and aapt2 have no INTERNET/ACCESS_NETWORK_STATE; verify_offline.sh passes |
| Reproducible source | Fresh checkout with pinned public llama.cpp submodule, no model/build output: release build passed, 54 tasks executed; subsequent source update rebuilt successfully |

The six default skips are candidate benchmark, final evaluation, receiver broadcast hook, destructive reset hook and two host-assisted recovery hooks. They require explicit arguments or host actions. They are not passing tests. Candidate/final benchmarks, real SMS capture and recovery were run separately as described below. Preparing the crash hook intentionally terminates its test process and is not a passing test; the subsequent recovery verification passed.

## Emulator acceptance matrix (plan §14.3)

| ID | Actual result and scope |
|---|---|
| E1 Consent/permissions | Pass: three-step UI, consent checkbox/timestamp, Android SMS and notification dialogs; denied-permission completion also tested. Home becomes Protected with monitoring, permission and model available. |
| E2 Provision/self-test | Pass: app-owned ADB provisioning and real SAF import of 1,117,320,736 bytes; progress observed at 274 MB. Imported SHA-256 matched the pinned source. Installed R8 self-test: 5,442 ms load, 701 prompt tokens at 71.5 tok/s, 131 generated tokens at 13.4 tok/s, 25.1 s total, offline. |
| E3 Foreground 10-SMS suite | Pass: 10/10 captured; eight flagged completed actual AI and two benign stayed SCREENED/Rule check. |
| E4 Background/alert tap | Pass: repeated 10/10 captured with UI away, eight real AI completions/two rule checks. Installed release private warning progressed from Rule check to On-device AI; tapping opened matching SMS detail with three verified quotes and actual 11.8 s provenance. |
| E5 Multipart | Pass: initial 348-character/three-PDU assembly; 273-character Filipino message completed AI; separate 171-character Unicode SMS with ñ, curly quotes and ₱ matched its full sent text in exactly one Room record and completed AI in 12,838 ms. This last harmless example was falsely classified as family impersonation. |
| E6 Duplicates | Pass within the implemented minute bucket: two identical sends produced one Room row. This is coarse `timestamp / 60,000` dedup, not a rolling 60-second window; a minute boundary can allow two records even less than 60 seconds apart. |
| E7 Offline | Pass: repeated receiver/real inference checks with Wi-Fi and mobile data off; cellular SMS radio remained available. |
| E8 Permission audit | Pass: installed release and APK manifest have no INTERNET/ACCESS_NETWORK_STATE. |
| E9 Revoke SMS | Pass: revoked RECEIVE_SMS, sent unique test SMS, no new row. Home showed monitoring off, permission Not allowed and Fix; no crash. Permission restored for later checks. |
| E10 Deny alerts | Pass: revoked POST_NOTIFICATIONS; a new scam SMS reached DONE, Qwen model recorded, 33,399 ms. No app NotificationRecord appeared; Home showed Alerts Off/Fix. Permission restored. |
| E11 Monitor off | Pass: used Home switch, sent unique test SMS, confirmed no Room row, then restored monitoring. |
| E12 Model missing | Pass: temporarily renamed both known model copies, reopened app and sent scam SMS. AI_UNAVAILABLE with no AI provenance; detail showed Rule check, low confidence and Install model. Both copies restored. |
| E13 Process interruption | Pass using `am crash`: `am kill` did not terminate the active JobService. Fresh native-busy marker, PID death confirmed, reopened process recovered saved work → DONE, 39,993 ms real inference and verified quotes; independent assertion passed. An earlier check restarted the target again through instrumentation and timed out; it is excluded from the passing result. |
| E14 Force-stop | Pass: unique SMS was absent from app storage while force-stopped; Android's stock Messages still received it. Opening the app restored capture for later broadcasts. |
| E15 Reboot | Pass: rebooted emulator, sent unique benign SMS without opening app; manifest receiver started the process and captured it. Permissions/monitoring persisted. |
| E16 Injection | Pass: latest installed R8 release received an actual injection SMS in the background → DONE/LIKELY_SCAM, 12,309 ms, two verified quotes and AI_MANIPULATION retained by deterministic fusion; unit tests cover the minimum SUSPICIOUS verdict and prompt escaping. |
| E17 Delete | Pass: Compose deletion confirmation removed a MANUAL record; Room assertions cover delete-record/delete-all. Release Settings delete-all UI reset consent/monitoring and retained the model; after re-consenting and receiving one new SMS, a debug data audit confirmed every prior record was gone (one new row only). |
| E18 Log audit | Pass: distinct word from a received test SMS absent from app-PID logcat. Native content logging is disabled; SafeLog accepts numeric metadata. Stock Messages/system-process logs are outside this app's claim. |

## Actual model evaluation and limitations

All three planned candidate models were compared in the technical spike. Qwen 1.5B won the specified selection rule. The completed final production pipeline evaluated 20 synthetic fixtures in 274.832 s: 19/20 matching binary decisions, 14 actual structured AI completions, six rule-only records, 30 verified quotes, zero dropped quotes or native failures. The receipt fixture was a false positive. See [benchmarks and numeric records](BENCHMARKS.md).

These same fixtures were used during prompt development, not as a held-out evaluation. Quote verification establishes correspondence to text, not reasoning truth. Model categories, explanations, Filipino prose and confidence can be wrong; the harmless Unicode multipart example above is another observed false positive. No broad detection accuracy claim is justified.

## Failures encountered and repaired

Initial native smoke skipped because a shell-owned model directory was inaccessible; reprovisioning under app ownership enabled actual inference. An early worker assertion failed when Qwen omitted optional evidence; flagged completions now request a real evidence object, which still must pass validation. Localized Context initially hid ActivityResultRegistryOwner; preserve the original owner. Old Espresso failed on API 37; pin 3.7.0. JobScheduler preemption initially left a waiting row AI_FAILED; conditionally requeue OS-stopped work while preserving explicit cancellation. All repairs have actual subsequent passing checks. See [gotchas](GOTCHAS.md) and [decisions](DECISIONS.md).

Host contention can cause large latency variation and a legitimate AI_TIMEOUT; no runtime timeout was increased to make a test pass. The optimized CPU path works on this emulator, but the unsupported-CPU guard has not been exercised on an older physical CPU.

## Unverified / human steps

The single physical-phone courier re-analysis is recorded below; carrier delivery, OEM battery behavior and sustained phone memory/thermal/battery behavior remain untested. No public remote was supplied. Public repository/release publication, live demo recording/upload, verification of required social wording and participant form submission remain the human-confirmed handoff in [implementation status](IMPLEMENTATION_STATUS.md) and [demo script](DEMO_SCRIPT.md). Historical overnight times in the plan are not claimed completion timestamps.

## v1.0.1 COD notice regression — 2026-10-10

A user reported a legitimate courier notice classified as likely_scam. The original rules added company + delivery + payment mentions (40 points); the AI could then mistake ordinary COD/photo-proof wording for deception. No sender authenticity is inferred from this report or the new context classifier.

The revised screening recognizes COD paid when the rider arrives, provided no independent danger is present. Links (including non-suspicious domains), secrets, advance fees/transfers, threats, urgency and injection disable the routine exception. ALL scope still performs real AI; a routine-context/model alarm is low-confidence UNCERTAIN with disagreement. Re-run re-screens stored masked text, allowing an old result to be corrected without reading inbox history. The fixture substitutes synthetic tracking/rider details for the user's personal message.

59 JVM tests passed with zero skips/failures, including the original 20-fixture AI-gating checks and nine new courier/attack/fusion cases. Debug/test/R8 release builds and release lint passed. The explicit `DeliveryNoticeRegressionTest` passed in 45.462 seconds on the provisioned emulator; [actual numeric records](courier-regression-results.json):

| Case | Actual result |
|---|---|
| Default flagged-only COD | SCREENED, LIKELY_SAFE, Rule check, no AI provenance |
| ALL / Filipino | DONE, LIKELY_SAFE, 11,929 ms real Qwen, no disagreement |
| ALL / English | DONE, LIKELY_SAFE, 9,575 ms real Qwen, no disagreement |
| Re-run old payment rules | Re-screened at score zero, DONE, LIKELY_SAFE, 9,816 ms real Qwen |
| COD plus advance-fee phishing link | DONE, LIKELY_SCAM, 13,974 ms real Qwen, three verified quotes |

The v1.0.1 default provisioned suite also passed: 10 active passes, seven explicit optional skips, zero failures, 46.133 seconds ([individual results](device-test-results-v1.0.1.json)). The separately enabled courier test above is an additional pass.

These are targeted synthetic regressions, not independent accuracy measurements. The earlier 19/20 evaluation belongs to v1.0.0 and is not claimed for this revised prompt. Package signing/alignment remain valid; the same model is used.

Installed the signed v1.0.1 update on the connected TECNO LH8n (Android 14 / API 34); package versionCode 2 was confirmed, the full 1,117,320,736-byte model file remained present, and installed network-permission audit passed. This installation check does not establish carrier/OEM reliability.

Re-ran the exact reported notice already saved in SALAG TRACE on that phone. Its detail changed to LIKELY_SAFE with actual Qwen2.5 1.5B / Q4_K_M inference taking 47.8 seconds offline ([non-content result metadata](phone-courier-regression.json)). This checks the user's existing record through the production re-run path; no carrier inbox was read and no SMS was sent. Temporary app UI dumps were removed, and personal tracking/rider details are excluded from repository artifacts. One successful phone inference does not establish sustained performance or broad detection accuracy.
