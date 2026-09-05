# Technology Stack

> 本文档由 acquire-codebase-knowledge Skill 于 2026-09-03 基于实测生成。所有结论均可追溯到具体文件或终端输出。
> 仓库：`Black0Bag/minibile`（fork 自 `AAswordman/Operit`），分支 `main`，HEAD `f323d6c5`。

## Core Sections (Required)

### 1) Runtime Summary

| Area | Value | Evidence |
|------|-------|----------|
| Primary language | Kotlin（1397 个 `.kt`，444,853 行） | `docs/codebase/.codebase-scan.txt` CODE METRICS；`find app/src/main/java -name '*.kt' \| xargs wc -l` |
| Secondary languages | TypeScript 225 / JavaScript 151 / C++ 46 / C Header 42 / Java 34 / Python 30 | 同上 CODE METRICS |
| Runtime | Android 应用，minSdk 26（Android 8.0），targetSdk 34，compileSdk 36，仅 `arm64-v8a` | `app/build.gradle.kts:398-410` |
| JVM 目标 | JVM_17（构建需 JDK 21） | `app/build.gradle.kts:574`；`docs/doc-src/dev-core/BUILDING.md:44` |
| 应用标识 | `com.ai.assistance.operit`，versionCode 46，versionName `1.12.1+3` | `app/build.gradle.kts:397-401` |
| Package manager | Gradle（Android/Kotlin）+ npm/pnpm（脚本与 web-chat）+ pip（CI 脚本） | `settings.gradle.kts`、`package.json`、`pnpm-workspace.yaml` |
| Module/build system | Gradle Kotlin DSL 多模块 + Version Catalog；AGP 8.13.2 / Kotlin 2.2.21 | `gradle/libs.versions.toml`；`settings.gradle.kts` |
| Monorepo 信号 | `pnpm-workspace.yaml`（含 `tools/mcp_bridge` 子 importer） | `pnpm-lock.yaml:406` |

Gradle 模块共 9 个（根 `settings.gradle.kts:480-493`）：

| Gradle 模块 | 物理目录 | 作用 |
|---|---|---|
| `:app` | `app/` | 主 Android 应用，全部业务逻辑 |
| `:dragonbones` | `avator/dragonbones/` | DragonBones 骨骼动画（C++/OpenGL/JNI） |
| `:terminal` | `terminal/`（Git 子模块） | OperitTerminalCore，内置 Ubuntu 24.04 PRoot 用户空间 |
| `:mnn` | `llm/mnn/` | 阿里 MNN 本地推理（CMake 拉上游源码） |
| `:llama` | `llm/llama/` | llama.cpp GGUF 本地推理 |
| `:mmd` | `avator/mmd/` | MMD 模型运行时（Bullet3 / Saba） |
| `:fbx` | `avator/fbx/` | FBX 解析（ufbx C 库） |
| `:showerclient` | `showerclient/` | Shower 虚拟显示客户端（Binder + 截图 + 触控注入） |
| `:quickjs` | `quickjs/` | QuickJS JNI 运行时（脚本引擎底座） |

### 2) Production Frameworks and Dependencies

仅列高影响生产依赖（`app/build.gradle.kts` dependencies 块）。

| Dependency | Version | Role in system | Evidence |
|------------|---------|----------------|----------|
| Jetpack Compose (BOM) | `2026.02.01` | 全量 UI，1351 处 `@Composable` | `gradle/libs.versions.toml`；`grep -c '@Composable'` |
| Material 3 + window size class | BOM 管理 | 设计系统与自适应布局（手机/平板双布局） | `ui/main/layout/PhoneLayout.kt`、`TabletLayout.kt` |
| Room | AndroidX | 关系型存储：聊天/消息/变体/Token 统计，**DB version 21，22 个迁移** | `data/db/AppDatabase.kt:20-28` |
| ObjectBox | 5.3.0 | 记忆图谱与文档分块（NoSQL，多 profile 多 store） | `build.gradle.kts:2`；`data/db/ObjectBox.kt` |
| DataStore (Preferences) | AndroidX | 38 个偏好管理器（模型配置/主题/权限/语音等） | `data/preferences/` 目录；23 个文件引用 DataStore |
| kotlinx.coroutines + Flow | 1.x | 全链路异步；自研 `util/stream` 流式管道 | `util/stream/Stream.kt` 等 12 文件 |
| kotlinx.serialization | 1.9.0 | 主序列化方案（另有 Gson/Moshi/HJSON 并存） | `gradle/libs.versions.toml` |
| OkHttp + Retrofit 2.9.0 + Moshi | — | HTTP 客户端与 LLM 流式请求 | `app/build.gradle.kts` dependencies |
| Ktor client (okhttp) | — | MCP SDK client 传输层 | `libs.mcp.sdk.client`、`libs.ktor.client.okhttp` |
| MCP Kotlin SDK | — | Model Context Protocol 本地/远程服务接入 | `data/mcp/MCPLocalServer.kt` |
| NanoHTTPD | — | 局域网 Web Chat / HTTP API / A2A server | `integrations/http/ExternalChatHttpServer.kt` |
| Shizuku API (`moe.shizuku`) | — | ADB 级调试权限通道 | `core/tools/system/ShizukuAuthorizer.kt` |
| libsu 6.0.0 | 6.0.0 | Root 通道（core/service/nio） | `app/build.gradle.kts` |
| ML Kit text recognition | — | OCR（中/日/韩/天城文） | `util/OCRUtils.kt` |
| Filament 1.69.2 | 1.69.2 | glTF/GLB 虚拟形象渲染 | `app/build.gradle.kts` |
| ExoPlayer / Media3 | — | 视频背景与媒体播放 | `app/build.gradle.kts` |
| FFmpegKit（本地 AAR） | vendored | 音视频转码工具（`libs/ffmpeg-kit-local.aar`） | `app/build.gradle.kts`；`util/FFmpegUtil.kt` |
| hnswlib-core 1.2.1 | 1.2.1 | 向量近邻检索（记忆库语义搜索） | `util/vector/VectorIndexManager.kt` |
| Jieba Android | — | 中文分词（混合检索的词法侧） | `util/TextSegmenter.kt` |
| BouncyCastle jdk18on 1.78 | 1.78 | APK 签名与加密（排除 jdk15to18 避免重复类） | `app/build.gradle.kts`；`core/subpack/KeyStoreHelper.kt` |
| apksig / apk-parser / axml / zipalign | — | APK 逆向与重打包（子包功能） | `core/subpack/ApkEditor.kt`、`ApkReverseEngineer.kt` |
| androidx.security-crypto 1.1.0-alpha06 | alpha | 加密偏好存储 | `app/build.gradle.kts` |
| Glance appwidget + material3 | — | 桌面小组件（语音助手 / ToolPkg 组件） | `widget/` 目录 7 文件 |
| Coil / Glide | — | 图片加载（两套并存） | `app/build.gradle.kts` |
| POI / PDFBox / itextg / zip4j | — | 文档解析与转换 | `util/DocumentConversionUtil.kt` |
| jlatexmath / RenderX | — | LaTeX 渲染 | `util/LatexMathMlConverter.kt` |

### 3) Development Toolchain

| Tool | Purpose | Evidence |
|------|---------|----------|
| Gradle Wrapper | BUILD（`./gradlew assembleDebug` / `assembleDebugClone`） | `gradlew`、`build.gradle.kts:232` |
| JUnit 4 + Mockito(+kotlin) + coroutines-test | TEST（129 个 JVM 单测） | `app/src/test/`；`app/build.gradle.kts` |
| Espresso + Compose UI test | TEST（androidTest，67 个 kt/js） | `app/src/androidTest/` |
| org.json:json 20240303 | TEST（Android org.json 在 JVM 是抛异常桩，必须替换） | `app/build.gradle.kts` 注释明示 |
| TypeScript 5.9.3 + esbuild 0.25 | BUILD（examples 脚本包编译） | `package.json` devDependencies |
| pnpm | BUILD（`sync_example_packages.py` 预构建依赖） | `pnpm-lock.yaml`、BUILDING.md:31 |
| Vite + React | BUILD（`web-chat/` 前端） | `web-chat/vite.config.ts` |
| Python 3 CI 脚本 | LINT/CHECK（本地化/Markdown 链接/仓库卫生/PR 分类） | `ci/script/*.py`、`ci/test/*.py` |
| GitHub Actions | CI（4 个 workflow） | `.github/workflows/` |
| **无 ktlint / detekt / spotless / editorconfig** | — | 实测：根目录及二级目录均未找到配置文件 |
| **Android Lint 已在 CI 移除/跳过** | — | `docs/TODO/ci_remove_android_lint_20260729/`、`ci_skip_android_lint_20260806/` |

### 4) Key Commands

```bash
# 依赖准备（必需顺序）
npm install                              # 根脚本依赖（typescript/esbuild）
npm --prefix web-chat install            # web-chat 前端依赖
git submodule update --init --recursive terminal   # 公开子模块

# 资源预构建（改动后必须重跑，否则 APK 里仍是旧产物）
npm run build:webchat                    # web-chat -> app/src/main/assets/web-chat
python3 ./tools/example_packages/sync_example_packages.py   # examples -> assets/packages

# 构建
./gradlew assembleDebug                  # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebugClone             # 共存版（applicationIdSuffix .clone）
./gradlew :app:compileDebugKotlin        # 只做 Kotlin 编译自检（推荐改工具签名后跑）

# 测试
./gradlew :app:testDebugUnitTest         # JVM 单测
python3 -m pytest ci/test/               # CI 脚本自测
```

### 5) Environment and Config

- 配置来源：
  - `local.properties`（**不入库**）：`sdk.dir`、`RELEASE_STORE_*`、`APK_ROTATION_*`（Release/Nightly 密钥轮换签名必填）
  - `app/config/stt-model-assets.properties`：本地 STT 模型的 URL + 字节数 + SHA256，构建时自动下载校验
  - `gradle.properties`：JVM 8GB、并行、缓存、workers.max=16
  - `gradle/libs.versions.toml`：统一版本目录
  - `local.properties.example`：可参考的模板
- 必需环境（构建机）：JDK 21、Android SDK platform-34 + build-tools 34.0.0（签名轮换额外需要 35.0.0）、NDK 25.1.8937393、Node.js + npm + pnpm、Python 3、宿主机 gcc/g++（MNN 需要编译宿主 flatc）
- **无 `.env.example`**：运行期 API Key 由用户在应用内配置并落 DataStore，不走环境变量
- 三份手动依赖归档必须自行下载放置，否则构建失败：`subpack.zip` → `app/src/main/assets/subpack/`、`jniLibs.zip` → `app/src/main/jniLibs/`、`libs.zip` → `app/libs/`
  - **当前克隆状态实测：这三处只有 `.keep` 占位文件，未放置依赖 → 现在直接构建会失败**

### 6) Evidence

- `docs/codebase/.codebase-scan.txt`（scan.py 全量输出）
- `settings.gradle.kts`、`build.gradle.kts`、`app/build.gradle.kts`、`gradle/libs.versions.toml`
- `gradle.properties`、`app/config/stt-model-assets.properties`、`local.properties.example`
- `docs/doc-src/dev-core/BUILDING.md`、`docs/doc-src/dev-core/CONTRIBUTING.md`
- `.github/workflows/{android-build,android-tests,pr-check,close-title-only-issues}.yml`
