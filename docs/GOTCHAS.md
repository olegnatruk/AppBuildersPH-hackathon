# Gotchas

## Environment

The system JDK must not be used: source `scripts/env.sh` for Android Studio's JBR 21. `adb` was initially absent from PATH. The initial Pixel_8 AVD has only 2048 MB RAM and 6 GB storage; real model inference needs the memory override in the plan.

## Android

Force-stopping prevents subsequent SMS broadcasts until the app is opened again. This is a documented platform limitation in the implementation plan, not a monitoring guarantee.

## Model provisioning

Creating `/sdcard/Android/data/ph.salagtrace` with `adb shell mkdir` before the app creates its external files directory gave the shell ownership and made the model invisible to the app. The smoke test correctly skipped (JUnit reports OK even for an assumption skip). Launch the app first so it creates `files/models`, then push the model. Inspect raw instrumentation status: code -4 means skipped, code 0 means passed. The directory was recreated by the app and real inference then passed.

## Structured inference

The initial Qwen spike prompt produced zero evidence objects across 20 runs despite valid JSON. The first full worker run therefore reached DONE with low confidence and no verified evidence, and the assertion requiring a trace failed. Keep this failure visible: the full prompt now supplies concise harmless/risky JSON examples and explicitly requests an exact quote for risky verdicts. Never fill missing evidence with fabricated or rule-derived AI claims. Fil/EN examples also reinforce the requested explanation language.

## Locale context and permission launchers

An emulator launch crashed with `No ActivityResultRegistryOwner` after LocalContext was replaced by a configuration context. Preserve LocalActivityResultRegistryOwner from the original Activity in the theme provider. Compilation alone did not catch this runtime issue. The failed launch is not a passing UI check.

## Gradle device tests may remove app/model storage

The connected-test runner uninstalled the target APK at teardown on this host, deleting its external model folder. A subsequent run skipped real-model tests. Those assumption skips are not native passes. For a provisioned emulator, install both APKs and invoke `adb shell am instrument -w -r ph.salagtrace.test/androidx.test.runner.AndroidJUnitRunner` directly; it preserves app/model storage. Reprovision after a Gradle teardown. The final provisioned suite passed 10 tests with 6 explicit optional skips; see TEST_REPORT.md for separately invoked checks.

## Background quota can stop work before inference

The ten-SMS foreground/background burst completed its SMS jobs, but JobScheduler interrupted a queued evaluation worker before native inference. Its replacement skipped the AI_FAILED row. Preserve QUEUED_AI for OS-stopped work and mark explicit cancellation before calling WorkManager cancel. StoppedWorkerTest exercises a real worker queued behind real native inference and verifies the conditional reset. The exploratory evaluation was aborted and rerun with the repair; its earlier rule-only failure is not counted as a successful AI run.

## Testing a process crash without restarting inference twice

`am kill` did not terminate the emulator process while its active JobService was running. The recovery check instead used `am crash` immediately after a fresh native-busy marker, confirmed the PID died, reopened the app, and observed its saved row. Starting a second instrumentation invocation immediately after reopening also restarts the target; that exploratory attempt timed out and is not a passing recovery check. Observe completion first, then invoke the optional verification assertion. The corrected check reached DONE with real model output and verified evidence (39,993 ms inference). Keep the normal native timeout; a busy host can legitimately produce a rule-only AI_TIMEOUT.
