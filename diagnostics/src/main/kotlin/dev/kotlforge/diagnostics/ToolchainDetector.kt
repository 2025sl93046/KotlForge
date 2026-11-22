package dev.kotlforge.diagnostics

import dev.kotlforge.core.Diagnostic
import dev.kotlforge.core.DiagnosticLevel
import dev.kotlforge.core.ToolchainInfo
import java.io.File

class ToolchainDetector {

    companion object {
        /** Standard SDK locations tried when no env var is set, in priority order. */
        internal val STANDARD_SDK_PATHS: List<String> = listOf(
            "${System.getProperty("user.home")}/Library/Android/sdk",          // macOS (Android Studio)
            "${System.getProperty("user.home")}/Android/Sdk",                  // Linux (Android Studio)
            "/opt/homebrew/share/android-commandlinetools",                     // macOS Homebrew
            "/usr/lib/android-sdk",                                             // Linux package manager
            "${System.getenv("LOCALAPPDATA")}\\Android\\Sdk"                   // Windows
        )
    }

    fun detect(projectDir: File? = null): ToolchainInfo {
        return ToolchainInfo(
            jdkPath = System.getenv("JAVA_HOME"),
            androidSdkPath = resolveAndroidSdkPath(),
            gradleVersion = projectDir?.let { readGradleWrapperVersion(it) },
            agpVersion = projectDir?.let { readAgpVersion(it) }
        )
    }

    internal fun resolveAndroidSdkPath(): String? {
        // 1. Explicit env vars take priority
        val fromEnv = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (fromEnv != null) return fromEnv

        // 2. Fall back to standard platform paths
        return STANDARD_SDK_PATHS.firstOrNull { File(it).isDirectory }
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
            !File(info.jdkPath).exists() ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.ERROR,
                    message = "JDK: path does not exist — ${info.jdkPath}",
                    suggestion = "Check JAVA_HOME points to a valid JDK installation"
                ))
            else ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.OK,
                    message = "JDK: ${info.jdkPath}"
                ))
        }

        when {
            info.androidSdkPath == null ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.ERROR,
                    message = "Android SDK: NOT FOUND — ANDROID_HOME is not set",
                    suggestion = "Run: export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools"
                ))
            !File(info.androidSdkPath).exists() ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.ERROR,
                    message = "Android SDK: path does not exist — ${info.androidSdkPath}",
                    suggestion = "Check ANDROID_HOME points to a valid Android SDK installation"
                ))
            else -> {
                val sdkPath = info.androidSdkPath!!
                result.add(Diagnostic(
                    level = DiagnosticLevel.OK,
                    message = "Android SDK: $sdkPath"
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
}
