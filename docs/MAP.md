# Repository map

The repository began with only `IMPLEMENTATION_PLAN.md`. There was no application, build configuration, CI, or test runner. This map is updated as those are implemented.

- `IMPLEMENTATION_PLAN.md`: product, architecture, task order, acceptance tests, and submission requirements.
- `PROJECT-BRIEF.md`: audience and scope.
- `scripts/env.sh`: local JBR 21 / Android SDK environment.
- `docs/DECISIONS.md`: architectural decisions fixed by the plan.

Current implementation status: setup. Planned paths in the plan are not yet implemented facts.

## Android build

`settings.gradle.kts` includes `:app`; `gradle/libs.versions.toml` pins dependency versions. `app/build.gradle.kts` sets package `ph.salagtrace`, Android 29 minimum / 36 compile and target, JVM 17. `MainActivity.kt` is currently a technical-spike placeholder. `app/src/main/res/xml` disables backup and transfer. The wrapper uses Gradle 8.13. App libraries match implementation plan §2.4.
