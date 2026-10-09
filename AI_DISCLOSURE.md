# AI disclosure

## Development assistance

The user supplied `IMPLEMENTATION_PLAN.md` and requested brain-skill initialization followed by implementation. The supplied plan attributes its planning to Claude Code; that authorship has not been independently verified. OpenAI Codex implemented the Kotlin/Compose application, JNI integration, tests, scripts and project documentation, with local build and emulator verification. Brain-skill initialized and maintained the repository's project memory (`AGENTS.md`, project brief and focused docs).

AI assistance does not establish correctness. The repository records observed test failures, repairs, real measurements and unverified physical-device/publication tasks. No synthetic timings or canned model outputs are substituted for inference. The launcher/shield artwork is a locally authored vector; IBM Plex fonts are bundled under their license. No external generative image service was used.

## Runtime inference

The selected model is Qwen2.5-1.5B-Instruct GGUF Q4_K_M. llama.cpp v0.6.0 runs it entirely on the Android device, using the model's chat template and grammar-constrained JSON. The model is provisioned separately and is not included in the APK/repository. Optional smaller Qwen/Gemma models were compared in the recorded spike; exact revisions/checksums are pinned.

All new SMS first passes deterministic rules. Depending on the user's scope setting, flagged or all messages then use local AI. Sensitive-value masking occurs before database storage and inference. Every displayed AI evidence quote is matched against the masked text; unsupported quotes are dropped, and fusion lowers confidence or flags disagreement. Rule-only, unavailable, failed and timed-out cases remain labeled as rule checks.

The model's category, reasoning, explanation and confidence can be wrong, even when its quote exists in the text. Confidence is uncalibrated. Small-model Filipino prose can be poor. Recommended actions are deterministic text, not model-generated instructions. The app performs no cloud inference, has no internet permission, and collects no analytics. It never automatically opens links or acts on AI conclusions.

See `docs/BENCHMARKS.md` and `docs/TEST_REPORT.md` for evidence and limits. This prototype has not been validated for production scam detection or physical carrier/OEM reliability.
