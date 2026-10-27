# Infinite Tokens 2026

An independent, open-source Android coding chat that runs SmolLM2 on-device. It borrows the familiar chat, project, and model-picker layout from the supplied mobile reference while using its own name and icon. It is not affiliated with Anthropic or Claude.

## What it does

- Keeps projects, chats, messages, selected model aliases, and Effort modes on the device.
- Runs the SmolLM2-135M-Instruct Q4_K_M model on-device through llama.cpp after you download it.
- Streams responses and lets you copy fenced code blocks.
- Works offline after the one-time model download. The app requests Internet access only for that download.
- On first launch, asks before downloading the model; the prompt shows its actual size. Choosing the second button exits the app.

The model is small and English-focused. Russian replies and generated code can be unreliable. Replies use an intentionally angry, profane voice. `Lou` through `Maxim` also prompt the model to make mistakes in any kind of answer; `Ultcod` removes those mistake instructions. These prompts cannot guarantee a particular answer or error rate. Replies are limited to 128 generated tokens to keep them short.

## Model choices

Each visible alias runs the same local model file, `SmolLM2-135M-Instruct-Q4_K_M.gguf`:

| In-app name | Local backend |
| --- | --- |
| GPT 10 Astra | SmolLM2-135M-Instruct Q4_K_M |
| Fable 6.7 | SmolLM2-135M-Instruct Q4_K_M |
| Gemini 5 Argon | SmolLM2-135M-Instruct Q4_K_M |
| DeepSeek 6.7 flssh | SmolLM2-135M-Instruct Q4_K_M |
| GLM 9 Prime | SmolLM2-135M-Instruct Q4_K_M |

Effort choices are `Lou` (100% wrong), `Medum` (90%), `Hi . Gh` (70%), `Xhi` (50%), `Maxim` (25%), and `Ultcod` (no wrong-answer prompt). These rates are prompt targets, not guarantees. Every mode uses an angry, profane voice and answers in the user's language.

## Install the APK

Download `Claude.apk` from the [v0.1.9 GitHub Release](https://github.com/manrobloxianidk-star/claude-code-infinite-tokens-mod-apk/releases/tag/v0.1.9), then open it on Android 9 (API 28) or later. Android may ask you to allow installation from the browser or file manager. The debug APK is signed with the standard development key and supports `arm64-v8a` and `x86_64`.

The APK does not contain the model. On first launch, accept the prompt to download about 106 MB from Hugging Face; the app verifies the file and stores it privately. Later launches reuse that download, and chat works offline. Keep around 200 MB free for the APK and downloaded model.

## Build from source

Requirements:

- JDK 17
- Android SDK Platform 37.0 and Build Tools 36.0.0
- Android NDK 28.2.13676358
- CMake 3.31.6
- Network access for Gradle dependencies and the first-run model download

Assemble the installable debug APK. The model is downloaded by the app after first launch:

```bash
git clone https://github.com/manrobloxianidk-star/claude-code-infinite-tokens-mod-apk.git
cd claude-code-infinite-tokens-mod-apk
./gradlew :app:assembleDebug
```

Set `ANDROID_HOME` and `ANDROID_SDK_ROOT` if the SDK is not in the standard location. No model file is needed to build the APK. Gradle writes `app-debug.apk`; the release workflow publishes it as `Claude.apk`.

The build pins llama.cpp commit `620d2225cc436ab0b001e3200e590e3b286ef9ab`. Its archive SHA-256 is checked during CMake configuration. The `arm64-v8a` build also includes the llama.cpp-selected KleidiAI v1.31.0 CPU kernels. Gradle pins the Android, Kotlin, Compose, Room, and KSP versions in `gradle/libs.versions.toml` and the build files.

## Model source and integrity

- Repository: [`bartowski/SmolLM2-135M-Instruct-GGUF`](https://huggingface.co/bartowski/SmolLM2-135M-Instruct-GGUF)
- Pinned revision: `d255afaffd3441b95abca9b5cc4c819b93f66936`
- File: `SmolLM2-135M-Instruct-Q4_K_M.gguf`
- Size: 105,454,432 bytes
- SHA-256: `2e8040ceae7815abe0dcb3540b9995eaa1fa0d2ca9e797d0a635ae4433c68c2d`
- Model license: Apache-2.0

The app downloads this pinned file only after the user accepts the first-run prompt. It verifies the exact size and SHA-256 before loading the model. The GGUF file is never included in the APK.

## License

The application source is available under the MIT License. See [LICENSE](LICENSE), [LICENSES/Apache-2.0.txt](LICENSES/Apache-2.0.txt), and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for model and runtime notices.
