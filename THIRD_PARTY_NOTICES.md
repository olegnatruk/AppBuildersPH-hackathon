# Third-party notices

The application's original code is MIT; see `LICENSE`. Model files are not distributed with the APK or repository. Users must comply with the license of any model they import.

| Component | Version / source | License |
|---|---|---|
| llama.cpp | v0.6.0, `d81235049384534c167caea52b85a694f6103d14`, pinned submodule | MIT; `third_party/llama.cpp/LICENSE` |
| Qwen2.5 1.5B / 0.5B Instruct GGUF | Pinned Hugging Face revisions in download script | Apache 2.0; model repository LICENSE |
| Optional Gemma 3 1B IT GGUF | Benchmark alternative, not selected or bundled | Google Gemma Terms of Use; conversion repo/model card. Do not treat it as Apache 2.0. |
| IBM Plex Sans / Mono | Bundled IBM font TTFs | SIL Open Font License 1.1; `docs/licenses/IBM-Plex-OFL.txt` |
| AndroidX / Jetpack Compose / Material / Room / WorkManager / DataStore | Exact versions in `gradle/libs.versions.toml` | Apache 2.0 |
| Kotlin / kotlinx.coroutines / kotlinx.serialization | Exact versions in catalog | Apache 2.0 |
| libc++ shared runtime | Android NDK r28 LLVM runtime | Apache 2.0 with LLVM exceptions; bundled `libcxx-LLVM.txt` |
| Gradle wrapper / Android Gradle Plugin / KSP | Exact versions pinned | Apache 2.0 |
| JUnit / AndroidX Test / Espresso | Test-only dependencies | EPL 1.0 (JUnit 4); Apache 2.0 (AndroidX Test) |

Primary sources: [llama.cpp license](https://github.com/ggml-org/llama.cpp/blob/d81235049384534c167caea52b85a694f6103d14/LICENSE), [Qwen model](https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF), [Gemma terms](https://ai.google.dev/gemma/terms), [IBM Plex](https://github.com/IBM/plex), [AndroidX license](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/LICENSE.txt), [Kotlin license](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt).

The APK bundles full MIT, Apache 2.0, IBM Plex OFL and libc++ license texts in `app/src/main/assets/licenses`; Settings → About → Licenses displays them. The app's About screen summarizes these licenses and the AI development/runtime disclosure. Android itself, development tools and emulator system images have their respective distributor terms. No third-party trademarks imply endorsement.
