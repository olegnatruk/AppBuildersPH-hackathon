# Gotchas

## Environment

The system JDK must not be used: source `scripts/env.sh` for Android Studio's JBR 21. `adb` was initially absent from PATH. The initial Pixel_8 AVD has only 2048 MB RAM and 6 GB storage; real model inference needs the memory override in the plan.

## Android

Force-stopping prevents subsequent SMS broadcasts until the app is opened again. This is a documented platform limitation in the implementation plan, not a monitoring guarantee.

## Model provisioning

Creating `/sdcard/Android/data/ph.salagtrace` with `adb shell mkdir` before the app creates its external files directory gave the shell ownership and made the model invisible to the app. The smoke test correctly skipped (JUnit reports OK even for an assumption skip). Launch the app first so it creates `files/models`, then push the model. Inspect raw instrumentation status: code -4 means skipped, code 0 means passed. The directory was recreated by the app and real inference then passed.

## Structured inference

The initial Qwen spike prompt produced zero evidence objects across 20 runs despite valid JSON. The first full worker run therefore reached DONE with low confidence and no verified evidence, and the assertion requiring a trace failed. Keep this failure visible: the full prompt now supplies concise harmless/risky JSON examples and explicitly requests an exact quote for risky verdicts. Never fill missing evidence with fabricated or rule-derived AI claims. Fil/EN examples also reinforce the requested explanation language.
