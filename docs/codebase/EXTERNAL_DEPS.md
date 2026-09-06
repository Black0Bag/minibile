# 外部依赖归档存档记录

> 生成于 2026-09-03。本文件用于记录三份外部构建依赖的来源、校验值与部署方式。
> 本仓不再与上游同步，这三份归档的 Google Drive 链接一旦失效将无法恢复，请务必按下方"备份建议"自行留存。

## 一、为什么需要这三份归档

仓库源码不含以下二进制产物，它们体积大或编译耗时长，上游选择用外部归档分发：

| 归档 | 大小 | SHA256 | 构建阻断 |
|---|---|---|---|
| `libs.zip` | 15,222,473 B (14.5 MB) | `1625489c81bb6e3ec6d29d41c39a53cb985e997be474425be01f909a6df887f8` | ✅ 是 |
| `jniLibs.zip` | 2,350,153 B (2.2 MB) | `2e8a4efd78b4b9daf3274ee17ba38eb5f42fb57bf43381b63c55aaa1d3148b53` | ✅ 是 |
| `subpack.zip` | 35,269,539 B (33.6 MB) | `2953448c93b0e4e51a6434b96b0145af271efdf07bf99a54b24925727c8545ae` | ❌ 否（仅运行期功能） |

**SHA256 由本次下载实测生成，上游未提供官方校验值。** 后续重新下载时应比对这些值，若不一致说明上游更新了归档或内容被篡改。

## 二、归档内容清单（实测解包）

### libs.zip → `app/libs/`（2 个条目）

```
app/libs/.keep                        1 B
app/libs/ffmpeg-kit-local.aar    15,264,401 B
```

`ffmpeg-kit-local.aar` 内含 10 个 arm64-v8a 原生库（构建时逐个校验存在且非空）：

| 库 | 大小 | 作用 |
|---|---|---|
| `libavcodec.so` | 16,448,392 B | 音视频编解码器集合（最大的一个） |
| `libavfilter.so` | 5,475,696 B | 滤镜（缩放、裁剪、水印等） |
| `libavformat.so` | 5,315,568 B | 容器格式封装/解封装（mp4、mkv 等） |
| `libc++_shared.so` | 991,920 B | C++ 标准库运行时 |
| `libavutil.so` | 615,760 B | 公共工具函数 |
| `libswscale.so` | 490,120 B | 图像色彩空间与缩放转换 |
| `libffmpegkit.so` | 470,624 B | FFmpegKit 的 JNI 封装层 |
| `libswresample.so` | 214,264 B | 音频重采样 |
| `libavdevice.so` | 57,808 B | 设备输入输出 |
| `libffmpegkit_abidetect.so` | 30,464 B | ABI 检测 |

引用位置：`app/build.gradle.kts:595` → `implementation(files("libs/ffmpeg-kit-local.aar"))`

### jniLibs.zip → `app/src/main/jniLibs/`（6 个条目）

```
app/src/main/jniLibs/.keep                                 23 B
app/src/main/jniLibs/arm64-v8a/liboperit_ripgrep.so  3,599,272 B  ← 构建强校验对象
app/src/main/jniLibs/arm64-v8a/libc++_shared.so        991,912 B
app/src/main/jniLibs/armeabi-v7a/libc++_shared.so      600,852 B
app/src/main/jniLibs/x86/libc++_shared.so              983,744 B
app/src/main/jniLibs/x86_64/libc++_shared.so         1,039,104 B
```

- `liboperit_ripgrep.so`：Rust 编译的原生全文搜索引擎，供 `util/ripgrep/` 使用
- 三个非 arm64 架构的 `libc++_shared.so` 实际不会打进 APK（`abiFilters` 只保留 `arm64-v8a`），是归档冗余

**注意**：`libstreamnative.so` 与 `libtoolpkgwasm.so` 不在此归档中，它们由 `app/src/main/cpp/CMakeLists.txt` 在本地构建时编译生成。

### subpack.zip → `app/src/main/assets/subpack/`（3 个条目）

```
app/src/main/assets/subpack/.keep            23 B
app/src/main/assets/subpack/android.apk  48,139,093 B  (45.9 MB)
app/src/main/assets/subpack/windows.zip  11,412,717 B  (10.9 MB)
```

用途：运行期"把对话/项目导出成独立 APP"功能的模板包。仅被 `ui/features/chat/components/ExportDialogs.kt:782`（Android 模板）与 `:893`（Windows 模板）读取。构建流程完全不校验（`grep -c subpack app/build.gradle.kts` = 0）。

**注意**：这 45.9 MB 的 `android.apk` 会打进最终 APK 的 assets，是包体积的主要贡献者之一。若定制方向确定不需要"导出独立 APP"功能，删掉此归档可直接省下 56.8 MB。

## 三、下载方式（实测可用）

### 上游官方脚本

`ci/script/download_android_dependencies.sh` 使用 `gdown` 从 Google Drive 下载，硬编码文件 ID：

| 归档 | Google Drive 文件 ID |
|---|---|
| `libs.zip` | `1Va1os7PRpCF3xtTwfx5kO11D7eIAvARG` |
| `subpack.zip` | `1SQs_dVPD6ldvwteqoUVjvBLjTWvr5Fpv` |
| `jniLibs.zip` | `1-W4fjjUwoShnB8Rh9RT5Gl8sHiGyQUaM` |

该脚本依赖 `RUNNER_TEMP` 环境变量（GitHub Actions 专用），本地直接跑会失败。

### 本次实测使用的直连方式（无需 gdown）

关键点：
- **必须强制 IPv4**（`curl -4`）。本环境 `drive.google.com` 的 DNS 只返回 IPv6 地址且不可达，走 IPv6 会 100% 超时。
- 使用 `drive.usercontent.google.com` 直下端点，带 `confirm=t` 跳过大文件确认页。

```bash
mkdir -p /root/operit_deps && cd /root/operit_deps

for spec in \
  '1Va1os7PRpCF3xtTwfx5kO11D7eIAvARG:libs.zip' \
  '1SQs_dVPD6ldvwteqoUVjvBLjTWvr5Fpv:subpack.zip' \
  '1-W4fjjUwoShnB8Rh9RT5Gl8sHiGyQUaM:jniLibs.zip'
do
  id="${spec%%:*}"; name="${spec##*:}"
  curl -4 -sL --retry 3 --retry-delay 3 -m 900 -o "$name" \
    "https://drive.usercontent.google.com/download?id=${id}&export=download&confirm=t"
done

sha256sum *.zip   # 与本文第一节的值比对
```

## 四、部署方式

用上游官方脚本部署（会做安全校验：路径逃逸、符号链接、压缩比炸弹、重复条目、加密条目）：

```bash
cd <仓库根>
python3 ci/script/prepare_android_dependencies.py \
  --profile full \
  --archives /root/operit_deps \
  --repository .
```

`--profile` 取值：
- `jvm`：只解 `libs.zip`（够跑 JVM 单测）
- `full`：解全部三份（完整 APK 构建）

**行为提示**：该脚本会 `shutil.rmtree` 清空目标目录再解压，`app/libs/`、`app/src/main/jniLibs/`、`app/src/main/assets/subpack/` 下的既有内容（含 `.keep`）都会被替换。

脚本还会主动排除若干条目（`prepare_android_dependencies.py:22-38`），因为这些由 Gradle 依赖提供，避免重复：
- `app/libs/arsc.jar`、`smart-exception-common-0.2.1.jar`、`smart-exception-java-0.2.1.jar`
- 各架构的 `libpl_droidsonroids_gif.so`

## 五、构建阻断机制

`app/build.gradle.kts:170-206` 定义 `verifyExternallyBuiltNativeLibraries` 任务，`:561-564` 将其挂到 `preBuild`：

```kotlin
tasks.named("preBuild") {
    dependsOn(syncMainAssets)
    dependsOn(verifyExternallyBuiltNativeLibraries)
}
```

三项 `require()` 强校验：

| # | 校验对象 | 失败提示 |
|---|---|---|
| 1 | `src/main/jniLibs/arm64-v8a/liboperit_ripgrep.so` 存在且非空 | 提示跑 `tools/native_ripgrep/build_native_ripgrep.ps1` |
| 2 | `libs/ffmpeg-kit-local.aar` 存在且非空 | 提示跑 `tools/ffmpeg/build_ffmpeg_kit_wsl.sh` + `import_local_ffmpeg_kit.ps1` |
| 3 | 打开该 AAR，10 个 arm64 `.so` 条目均存在且 size > 0 | 列出缺失条目 |

任一失败 → `assembleDebug` 在 `preBuild` 阶段即失败，不进入编译。

## 六、自建路径（脱离 Google Drive）

| 产物 | 源码位置 | 现状 |
|---|---|---|
| `liboperit_ripgrep.so` | `tools/native_ripgrep/`（Rust，含 `Cargo.toml` + `Cargo.lock` + `src/`） | 现成脚本为 `build_native_ripgrep.ps1` / `.bat`（**仅 Windows**）。Linux 需自行用 `cargo` + `aarch64-linux-android` target + NDK 交叉编译 |
| `ffmpeg-kit-local.aar` | `tools/ffmpeg/build_ffmpeg_kit_wsl.sh` | WSL 脚本，从 FFmpeg 源码完整编译，**耗时以小时计** |
| `subpack/android.apk`、`windows.zip` | 无 | 无自建脚本，是预打包模板产物。若需要必须保留归档 |

## 七、备份建议

三份归档的唯一来源是上游作者的 Google Drive，存在以下风险：
- 链接失效（作者删除/改权限）
- 内容被替换（上游脚本只有 `test -s` 非空检查，无哈希校验）
- 网络不可达（需强制 IPv4，且可能需要代理）

**建议动作**：
1. 把 `/root/operit_deps/*.zip` 复制到你自己的持久存储（本地 NAS、私有对象存储、外置存储卡）
2. 连同本文第一节的 SHA256 一起存档
3. 恢复时先校验哈希再部署

当前归档暂存位置：`/root/operit_deps/`（Linux 终端环境，**非持久化**，环境重建会丢失）

## 八、Git 状态确认

这三处目录均在 `.gitignore` 中，二进制不会误入库（实测 `git check-ignore -v`）：

```
.gitignore:24  /app/src/main/jniLibs           → liboperit_ripgrep.so
.gitignore:26  /app/src/main/assets/subpack    → android.apk
.gitignore:27  /app/libs/*                     → ffmpeg-kit-local.aar
```

## 九、构建工具链现状（2026-09-03 实测）

三份归档已部署，但**完整 APK 构建仍缺工具链**。以下为 Linux 终端环境实测结果：

| 工具 | 状态 | 构建必需性 |
|---|---|---|
| `gcc` 13.3.0 | ✅ 已有 | MNN 需要它编译宿主机 `flatc` |
| `node` v24.19.0 | ✅ 已有 | web-chat 与 examples 构建 |
| `pnpm` 11.22.0 | ✅ 已有 | `sync_example_packages.py` 预构建 |
| `python3` | ✅ 已有 | CI 脚本、依赖部署 |
| **JDK 21** | ❌ **缺失**（`java: command not found`） | **必需**，Gradle 无法运行 |
| **Android SDK** | ❌ **缺失**（`~/Android` 不存在，`ANDROID_HOME` 未设置） | **必需**（platform-34 + build-tools 34.0.0） |
| **Android NDK 25.1.8937393** | ❌ **缺失** | **必需**（9 个模块含原生代码） |
| `cmake` | ❌ 缺失 | **必需**（原生模块构建） |

**结论**：当前只解决了"外部二进制依赖"这一层，构建还需安装 JDK 21 + Android SDK/NDK + cmake。按 `docs/doc-src/dev-core/BUILDING.md` 步骤 1-4 操作。

预估磁盘占用：JDK 21 约 300 MB + SDK/NDK 约 6-8 GB + Gradle 依赖缓存约 2-3 GB。当前可用 360 GB，空间充足。

**是否需要装取决于定制方向**：
- 若要真机验证改动 → 必须装
- 若只做代码级重构与设计讨论 → 可暂缓