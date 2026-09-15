package com.ai.assistance.operit.core.vibecoding.cloudbuild

/**
 * CI 模板注册表（Phase E 只读预览）。
 *
 * 每个模板声明检测规则、runner、工具链、测试/构建命令、产物 glob、签名需求。
 * 第三方 Actions 固定完整 commit SHA（在生成 workflow 时由调用方落实）；
 * 本类只保存声明与来源证据，不执行任何远端写入。
 */
object CloudBuildTemplateRegistry {
    private const val SOURCE_03A = "03a_vibecoding_task_engine_and_cloud_build_plan.md#6.4"

    val templates: List<CloudBuildTemplate> =
        listOf(
            CloudBuildTemplate(
                id = "android-gradle-apk",
                displayName = "Android Gradle APK",
                detectionMarkers = listOf("settings.gradle.kts", "settings.gradle", "app/build.gradle.kts", "build.gradle"),
                runner = "ubuntu-24.04",
                toolchain = listOf("JDK 21", "Android SDK", "NDK 25.1.8937393", "CMake 3.22.1"),
                testCommand = "./gradlew :app:testDebugUnitTest",
                buildCommand = "./gradlew :app:assembleRelease",
                artifactGlobs = listOf("app/build/outputs/apk/release/*.apk"),
                signatureRequired = true,
                version = 1,
                source = SOURCE_03A,
            ),
            CloudBuildTemplate(
                id = "flutter-android",
                displayName = "Flutter Android",
                detectionMarkers = listOf("pubspec.yaml", "pubspec.yml"),
                runner = "ubuntu-24.04",
                toolchain = listOf("Flutter stable", "JDK 21", "Android SDK"),
                testCommand = "flutter test",
                buildCommand = "flutter build apk --release",
                artifactGlobs = listOf("build/app/outputs/flutter-apk/*.apk"),
                signatureRequired = true,
                version = 1,
                source = SOURCE_03A,
            ),
            CloudBuildTemplate(
                id = "node-web",
                displayName = "Node.js Web",
                detectionMarkers = listOf("package.json", "pnpm-lock.yaml", "yarn.lock"),
                runner = "ubuntu-24.04",
                toolchain = listOf("Node.js 22"),
                testCommand = "pnpm test",
                buildCommand = "pnpm build",
                artifactGlobs = listOf("dist/**", "build/**"),
                signatureRequired = false,
                version = 1,
                source = SOURCE_03A,
            ),
            CloudBuildTemplate(
                id = "python-package",
                displayName = "Python Package",
                detectionMarkers = listOf("pyproject.toml", "requirements.txt", "setup.py"),
                runner = "ubuntu-24.04",
                toolchain = listOf("Python 3.12"),
                testCommand = "python -m pytest",
                buildCommand = "python -m build",
                artifactGlobs = listOf("dist/*"),
                signatureRequired = false,
                version = 1,
                source = SOURCE_03A,
            ),
            CloudBuildTemplate(
                id = "jvm-gradle",
                displayName = "JVM Gradle",
                detectionMarkers = listOf("build.gradle.kts", "build.gradle"),
                runner = "ubuntu-24.04",
                toolchain = listOf("JDK 21"),
                testCommand = "./gradlew test",
                buildCommand = "./gradlew build",
                artifactGlobs = listOf("build/libs/*", "build/distributions/*"),
                signatureRequired = false,
                version = 1,
                source = SOURCE_03A,
            ),
            CloudBuildTemplate(
                id = "jvm-maven",
                displayName = "JVM Maven",
                detectionMarkers = listOf("pom.xml"),
                runner = "ubuntu-24.04",
                toolchain = listOf("JDK 21", "Maven"),
                testCommand = "mvn test",
                buildCommand = "mvn package",
                artifactGlobs = listOf("target/*.jar"),
                signatureRequired = false,
                version = 1,
                source = SOURCE_03A,
            ),
            CloudBuildTemplate(
                id = "rust-binary",
                displayName = "Rust Binary",
                detectionMarkers = listOf("Cargo.toml"),
                runner = "ubuntu-24.04",
                toolchain = listOf("Rust 1.88.0"),
                testCommand = "cargo test",
                buildCommand = "cargo build --release",
                artifactGlobs = listOf("target/release/*"),
                signatureRequired = false,
                version = 1,
                source = SOURCE_03A,
            ),
            CloudBuildTemplate(
                id = "generic-command",
                displayName = "Generic Command",
                detectionMarkers = emptyList(),
                runner = "ubuntu-24.04",
                toolchain = emptyList(),
                testCommand = null,
                buildCommand = "make build",
                artifactGlobs = emptyList(),
                signatureRequired = false,
                version = 1,
                source = SOURCE_03A,
            ),
        )

    fun match(projectFiles: Collection<String>): CloudBuildTemplate? =
        templates
            .filter { it.id != "generic-command" }
            .maxByOrNull { template -> template.detectionMarkers.count { marker -> projectFiles.any { it.endsWith(marker) } } }
            ?.takeIf { matched -> matched.detectionMarkers.any { marker -> projectFiles.any { it.endsWith(marker) } } }
            ?: templates.first { it.id == "generic-command" }
}