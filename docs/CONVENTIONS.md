# Conventions

No pre-existing code conventions were present at bootstrap. The implementation follows the plan: Kotlin DSL and a version catalog, one `:app` module, manual dependency injection, pure Kotlin domain logic, Room persistence, and Compose UI.

Never log SMS content, persist raw sensitive values, label a rule result as AI, or invent inference timings. Never add network dependencies or INTERNET permission. Keep third-party models and local machine paths outside version control. Any extra dependency needs a written reason in `DECISIONS.md`.

Verification commands are documented in `AGENTS.md` when the build exists. Unit tests target meaningful masking, screening, evidence-validation, and fusion behavior. Device checks must distinguish measured passes from untested cases.
