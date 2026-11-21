package dev.kotlforge.diagnostics

import dev.kotlforge.core.Diagnostic
import dev.kotlforge.core.DiagnosticLevel
import java.io.File

data class ToolchainInfo(
    val jdkPath: String?,
    val androidSdkPath: String?,
    val gradleVersion: String?
)

class ToolchainDetector {

    fun detect(): ToolchainInfo {
        return ToolchainInfo(
            jdkPath = System.getenv("JAVA_HOME"),
            androidSdkPath = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT"),
            gradleVersion = null
        )
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
            else ->
                result.add(Diagnostic(
                    level = DiagnosticLevel.OK,
                    message = "Android SDK: ${info.androidSdkPath}"
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
}
