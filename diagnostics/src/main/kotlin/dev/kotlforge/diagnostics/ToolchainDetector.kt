package dev.kotlforge.diagnostics

import dev.kotlforge.core.Diagnostic
import dev.kotlforge.core.DiagnosticLevel
import dev.kotlforge.core.ToolchainInfo
import java.io.File

class ToolchainDetector(
    private val env: (String) -> String? = System::getenv,
    private val isDirectory: (String) -> Boolean = { File(it).isDirectory }
) {

    companion object {
        /** Static SDK locations (no env var expansion needed), in priority order. */
        internal val STANDARD_SDK_PATHS: List<String> = listOf(
            "${System.getProperty("user.home")}/Library/Android/sdk",   // macOS (Android Studio)
            "${System.getProperty("user.home")}/Android/Sdk",           // Linux (Android Studio)
            "/opt/homebrew/share/android-commandlinetools",              // macOS Homebrew
            "/usr/lib/android-sdk",                                      // Linux package manager
        )

        /**
         * Windows SDK paths that require env var expansion. Evaluated lazily via
         * [resolveAndroidSdkPath] so they go through the injected [env] provider.
         */
        internal val WINDOWS_SDK_PATH_TEMPLATES: List<String> = listOf(
            "%LOCALAPPDATA%\\Android\\Sdk",   // Android Studio default
            "%USERPROFILE%\\Android\\Sdk",    // fallback user profile
        )
    }

    fun detect(projectDir: File? = null): ToolchainInfo {
        return ToolchainInfo(
            jdkPath = env("JAVA_HOME"),
            androidSdkPath = resolveAndroidSdkPath(),
            gradleVersion = projectDir?.let { readGradleWrapperVersion(it) },
            agpVersion = projectDir?.let { readAgpVersion(it) }
        )
    }

    /** Single entry point: detect environment then return all structured checks. */
    fun runChecks(projectDir: File? = null): List<Diagnostic> {
        return checks(detect(projectDir))
    }

    /**
     * Redacts values that look like credentials from a diagnostic message.
     * Specifically strips:
     *  - KEY=VALUE pairs where VALUE is 8+ chars (env var leaks)
     *  - Standalone tokens: 20+ char alphanumeric strings
     */
    internal fun sanitize(text: String): String =
        text
            .replace(Regex("""(\w+=)[A-Za-z0-9+/]{8,}={0,2}"""), "$1[REDACTED]")
            .replace(Regex("""(?<![/\\.A-Za-z0-9])[A-Za-z0-9+/]{20,}(?![/\\.A-Za-z0-9])"""), "[REDACTED]")

    internal fun resolveAndroidSdkPath(): String? {
        // 1. Explicit env vars take priority
        val fromEnv = env("ANDROID_HOME") ?: env("ANDROID_SDK_ROOT")
        if (fromEnv != null) return fromEnv

        // 2. Static standard paths (macOS / Linux)
        STANDARD_SDK_PATHS.firstOrNull { isDirectory(it) }?.let { return it }

        // 3. Windows paths — expand %VAR% tokens via injected env
        return WINDOWS_SDK_PATH_TEMPLATES
            .mapNotNull { template -> expandWindowsTemplate(template) }
            .firstOrNull { isDirectory(it) }
    }

    /** Expands a single %VAR_NAME% token in a Windows path template. */
    internal fun expandWindowsTemplate(template: String): String? {
        val match = Regex("""%([^%]+)%""").find(template) ?: return template
        val value = env(match.groupValues[1]) ?: return null
        return template.replace(match.value, value)
    }

    fun checks(info: ToolchainInfo): List<Diagnostic> {
        val result = mutableListOf<Diagnostic>()

        when {
            info.jdkPath == null ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.ERROR,
                    message = "JDK: NOT FOUND — JAVA_HOME is not set",
                    suggestion = "Run: export JAVA_HOME=\$(/usr/libexec/java_home -v 17)"
                ))
            !File(info.jdkPath!!).exists() ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.ERROR,
                    message = "JDK: path does not exist — ${sanitize(info.jdkPath!!)}",
                    suggestion = "Check JAVA_HOME points to a valid JDK installation"
                ))
            else ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.OK,
                    message = "JDK: ${sanitize(info.jdkPath!!)}"
                ))
        }

        when {
            info.androidSdkPath == null ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.ERROR,
                    message = "Android SDK: NOT FOUND — ANDROID_HOME is not set",
                    suggestion = "Run: export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools"
                ))
            !File(info.androidSdkPath!!).exists() ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.ERROR,
                    message = "Android SDK: path does not exist — ${sanitize(info.androidSdkPath!!)}",
                    suggestion = "Check ANDROID_HOME points to a valid Android SDK installation"
                ))
            else -> {
                val sdkPath = info.androidSdkPath!!
                result.add(Diagnostic(
                    level = DiagnosticLevel.OK,
                    message = "Android SDK: ${sanitize(sdkPath)}"
                ))
                result.addAll(checkSdkPackages(sdkPath))
            }
        }

        when (info.gradleVersion) {
            null -> result.add(Diagnostic(
                level = DiagnosticLevel.WARNING,
                message = "Gradle wrapper: NOT FOUND — no gradle-wrapper.properties in project",
                suggestion = "Run: gradle wrapper --gradle-version 8.11"
            ))
            else -> result.add(Diagnostic(
                level = DiagnosticLevel.OK,
                message = "Gradle wrapper: ${info.gradleVersion}"
            ))
        }

        when (info.agpVersion) {
            null -> result.add(Diagnostic(
                level = DiagnosticLevel.WARNING,
                message = "AGP: NOT FOUND — could not detect Android Gradle Plugin version",
                suggestion = "Declare AGP version in libs.versions.toml or root build.gradle.kts"
            ))
            else -> result.add(Diagnostic(
                level = DiagnosticLevel.OK,
                message = "AGP: ${info.agpVersion}"
            ))
        }

        result.addAll(checkCompatibility(info))

        return result
    }

    fun validate(info: ToolchainInfo): List<String> {
        val errors = mutableListOf<String>()
        if (info.jdkPath == null) errors.add("JAVA_HOME is not set")
        if (info.androidSdkPath == null) errors.add("ANDROID_HOME is not set")
        return errors
    }

    internal fun checkSdkPackages(sdkPath: String): List<Diagnostic> {
        val result = mutableListOf<Diagnostic>()
        val sdk = File(sdkPath)

        // platforms/android-XX — at least one must be present
        val platformsDir = File(sdk, "platforms")
        val installedPlatforms = platformsDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("android-") }
            ?: emptyList()

        if (installedPlatforms.isEmpty()) {
            result.add(Diagnostic(
                level = DiagnosticLevel.ERROR,
                message = "Android SDK: no platform installed under ${platformsDir.path}",
                suggestion = "Run: sdkmanager \"platforms;android-35\""
            ))
        } else {
            result.add(Diagnostic(
                level = DiagnosticLevel.OK,
                message = "Android SDK platforms: ${installedPlatforms.map { it.name }.sorted().joinToString(", ")}"
            ))
        }

        // build-tools/XX.Y.Z — at least one must be present
        val buildToolsDir = File(sdk, "build-tools")
        val installedBuildTools = buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?: emptyList()

        if (installedBuildTools.isEmpty()) {
            result.add(Diagnostic(
                level = DiagnosticLevel.ERROR,
                message = "Android SDK: no build-tools installed under ${buildToolsDir.path}",
                suggestion = "Run: sdkmanager \"build-tools;35.0.0\""
            ))
        } else {
            result.add(Diagnostic(
                level = DiagnosticLevel.OK,
                message = "Android SDK build-tools: ${installedBuildTools.map { it.name }.sorted().joinToString(", ")}"
            ))
        }

        return result
    }

    internal fun readGradleWrapperVersion(projectDir: File): String? {
        val props = File(projectDir, "gradle/wrapper/gradle-wrapper.properties")
        if (!props.exists()) return null
        val distributionUrl = props.readLines()
            .firstOrNull { it.startsWith("distributionUrl=") }
            ?: return null
        // e.g. https\://services.gradle.org/distributions/gradle-8.11-bin.zip
        return Regex("gradle-(\\d+(?:\\.\\d+)+)-")
            .find(distributionUrl)
            ?.groupValues?.get(1)
    }

    internal fun readAgpVersion(projectDir: File): String? {
        // 1. libs.versions.toml — agp = "X.Y.Z"
        val toml = File(projectDir, "gradle/libs.versions.toml")
        if (toml.exists()) {
            val match = Regex("""agp\s*=\s*"([\d.]+)"""").find(toml.readText())
            if (match != null) return match.groupValues[1]
        }

        // 2. build.gradle.kts — id("com.android.application") version "X.Y.Z"
        //                       id("com.android.library") version "X.Y.Z"
        val buildFile = listOf(
            File(projectDir, "build.gradle.kts"),
            File(projectDir, "build.gradle")
        ).firstOrNull { it.exists() } ?: return null

        return Regex("""com\.android\.\w+["']\s*\)?\s*version\s*["']([\d.]+)""")
            .find(buildFile.readText())
            ?.groupValues?.get(1)
    }

    /**
     * Emits WARNING diagnostics for known bad toolchain version combinations.
     * Returns an empty list when versions are absent (can't evaluate) or compatible.
     */
    internal fun checkCompatibility(info: ToolchainInfo): List<Diagnostic> {
        val result = mutableListOf<Diagnostic>()

        val jdkMajor    = info.jdkVersion?.let { parseLeadingInt(it) }
        val gradleMajor = info.gradleVersion?.let { parseLeadingInt(it) }
        val agpMajor    = info.agpVersion?.let { parseLeadingInt(it) }
        val agpMinor    = info.agpVersion?.let { parseMinorInt(it) }

        // JDK 25+ is not supported by Gradle 8.x (breaks bytecode compatibility)
        if (jdkMajor != null && jdkMajor >= 25 && gradleMajor != null && gradleMajor < 9) {
            result.add(Diagnostic(
                level = DiagnosticLevel.WARNING,
                message = "Incompatible versions: JDK ${info.jdkVersion} is not supported by Gradle ${info.gradleVersion}",
                suggestion = "Use JDK 17 or 21, or upgrade to Gradle 9+"
            ))
        }

        // AGP 8.x requires Gradle 8.0+
        if (agpMajor != null && agpMajor >= 8 && gradleMajor != null && gradleMajor < 8) {
            result.add(Diagnostic(
                level = DiagnosticLevel.WARNING,
                message = "Incompatible versions: AGP ${info.agpVersion} requires Gradle 8.0+, found Gradle ${info.gradleVersion}",
                suggestion = "Upgrade Gradle to 8.0 or higher"
            ))
        }

        // AGP 7.x requires Gradle 7.0+
        if (agpMajor != null && agpMajor == 7 && gradleMajor != null && gradleMajor < 7) {
            result.add(Diagnostic(
                level = DiagnosticLevel.WARNING,
                message = "Incompatible versions: AGP ${info.agpVersion} requires Gradle 7.0+, found Gradle ${info.gradleVersion}",
                suggestion = "Upgrade Gradle to 7.0 or higher"
            ))
        }

        // AGP 8.2+ requires JDK 17+
        if (agpMajor != null && agpMinor != null &&
            (agpMajor > 8 || (agpMajor == 8 && agpMinor >= 2)) &&
            jdkMajor != null && jdkMajor < 17) {
            result.add(Diagnostic(
                level = DiagnosticLevel.WARNING,
                message = "Incompatible versions: AGP ${info.agpVersion} requires JDK 17+, found JDK ${info.jdkVersion}",
                suggestion = "Set JAVA_HOME to a JDK 17 or 21 installation"
            ))
        }

        return result
    }

    private fun parseLeadingInt(version: String): Int? =
        version.split(".").firstOrNull()?.toIntOrNull()

    private fun parseMinorInt(version: String): Int? =
        version.split(".").getOrNull(1)?.toIntOrNull()
}
