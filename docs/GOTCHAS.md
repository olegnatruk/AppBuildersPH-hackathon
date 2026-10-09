# Gotchas

## Environment

The system JDK must not be used: source `scripts/env.sh` for Android Studio's JBR 21. `adb` was initially absent from PATH. The initial Pixel_8 AVD has only 2048 MB RAM and 6 GB storage; real model inference needs the memory override in the plan.

## Android

Force-stopping prevents subsequent SMS broadcasts until the app is opened again. This is a documented platform limitation in the implementation plan, not a monitoring guarantee.
