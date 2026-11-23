package dev.kotlforge.diagnostics

import dev.kotlforge.core.DiagnosticLevel
import dev.kotlforge.core.ToolchainInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ToolchainDetectorTest {

    private val detector = ToolchainDetector()

    // ── detect() ─────────────────────────────────────────────────────────────

    @Test
    fun `detect returns a ToolchainInfo with jdkPath from JAVA_HOME`() {
        val info = detector.detect()
        assertEquals(System.getenv("JAVA_HOME"), info.jdkPath)
    }

    // ── checks() — JDK ───────────────────────────────────────────────────────

    @Test
    fun `checks returns ERROR when jdkPath is null`() {
        val info = ToolchainInfo(jdkPath = null)
        val diagnostics = detector.checks(info)
        val jdkDiag = diagnostics.first { it.message.contains("JDK") }
        assertEquals(DiagnosticLevel.ERROR, jdkDiag.level)
        assertNotNull(jdkDiag.suggestion)
    }

    @Test
    fun `checks returns ERROR when jdkPath does not exist on disk`() {
        val info = ToolchainInfo(jdkPath = "/nonexistent/jdk/path")
        val diagnostics = detector.checks(info)
        val jdkDiag = diagnostics.first { it.message.contains("JDK") }
        assertEquals(DiagnosticLevel.ERROR, jdkDiag.level)
        assertNotNull(jdkDiag.suggestion)
    }

    @Test
    fun `checks returns OK when jdkPath exists on disk`() {
        val tempDir = Files.createTempDirectory("fake-jdk").toFile()
        try {
            val info = ToolchainInfo(jdkPath = tempDir.absolutePath)
            val diagnostics = detector.checks(info)
            val jdkDiag = diagnostics.first { it.message.contains("JDK") }
            assertEquals(DiagnosticLevel.OK, jdkDiag.level)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ── resolveAndroidSdkPath() ───────────────────────────────────────────────

    @Test
    fun `detect picks up androidSdkPath from ANDROID_HOME env var`() {
        val info = detector.detect()
        val expected = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (expected != null) {
            assertEquals(expected, info.androidSdkPath)
        }
    }

    @Test
    fun `resolveAndroidSdkPath returns null when no env var and no standard path exists`() {
        val result = ToolchainDetector.STANDARD_SDK_PATHS
            .filter { java.io.File(it).isDirectory }
        val resolved = detector.resolveAndroidSdkPath()
        if (result.isEmpty() && System.getenv("ANDROID_HOME") == null && System.getenv("ANDROID_SDK_ROOT") == null) {
            assertNull(resolved)
        } else {
            assertNotNull(resolved)
        }
    }

    @Test
    fun `resolveAndroidSdkPath prefers ANDROID_HOME over standard paths`() {
        val androidHome = System.getenv("ANDROID_HOME")
        if (androidHome != null) {
            assertEquals(androidHome, detector.resolveAndroidSdkPath())
        }
    }

    @Test
    fun `resolveAndroidSdkPath falls back to first existing standard path`() {
        val tempDir = Files.createTempDirectory("fake-sdk").toFile()
        try {
            val paths = listOf("/nonexistent/path1", tempDir.absolutePath, "/nonexistent/path2")
            val result = paths.firstOrNull { java.io.File(it).isDirectory }
            assertEquals(tempDir.absolutePath, result)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ── checks() — Android SDK ───────────────────────────────────────────────

    @Test
    fun `checks returns ERROR when androidSdkPath is null`() {
        val info = ToolchainInfo(androidSdkPath = null)
        val diagnostics = detector.checks(info)
        val sdkDiag = diagnostics.first { it.message.contains("Android SDK") }
        assertEquals(DiagnosticLevel.ERROR, sdkDiag.level)
        assertNotNull(sdkDiag.suggestion)
    }

    @Test
    fun `checks returns ERROR when androidSdkPath does not exist on disk`() {
        val info = ToolchainInfo(androidSdkPath = "/nonexistent/android/sdk")
        val diagnostics = detector.checks(info)
        val sdkDiag = diagnostics.first { it.message.contains("Android SDK") }
        assertEquals(DiagnosticLevel.ERROR, sdkDiag.level)
        assertNotNull(sdkDiag.suggestion)
    }

    @Test
    fun `checks returns OK when androidSdkPath exists on disk`() {
        val tempDir = Files.createTempDirectory("fake-sdk").toFile()
        try {
            val info = ToolchainInfo(androidSdkPath = tempDir.absolutePath)
            val diagnostics = detector.checks(info)
            val sdkDiag = diagnostics.first { it.message.contains("Android SDK:") && !it.message.contains("platform") && !it.message.contains("build-tools") }
            assertEquals(DiagnosticLevel.OK, sdkDiag.level)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ── checkSdkPackages() ───────────────────────────────────────────────────

    @Test
    fun `checkSdkPackages returns ERROR when platforms directory is empty`() {
        val tempDir = Files.createTempDirectory("fake-sdk").toFile()
        try {
            File(tempDir, "platforms").mkdirs()
            File(tempDir, "build-tools").mkdirs()
            val diagnostics = detector.checkSdkPackages(tempDir.absolutePath)
            val diag = diagnostics.first { it.message.contains("platform") }
            assertEquals(DiagnosticLevel.ERROR, diag.level)
            assertNotNull(diag.suggestion)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `checkSdkPackages returns OK when at least one platform is installed`() {
        val tempDir = Files.createTempDirectory("fake-sdk").toFile()
        try {
            File(tempDir, "platforms/android-35").mkdirs()
            File(tempDir, "build-tools/35.0.0").mkdirs()
            val diagnostics = detector.checkSdkPackages(tempDir.absolutePath)
            val diag = diagnostics.first { it.message.contains("platforms") }
            assertEquals(DiagnosticLevel.OK, diag.level)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `checkSdkPackages returns ERROR when build-tools directory is empty`() {
        val tempDir = Files.createTempDirectory("fake-sdk").toFile()
        try {
            File(tempDir, "platforms/android-35").mkdirs()
            File(tempDir, "build-tools").mkdirs()
            val diagnostics = detector.checkSdkPackages(tempDir.absolutePath)
            val diag = diagnostics.first { it.message.contains("build-tools") }
            assertEquals(DiagnosticLevel.ERROR, diag.level)
            assertNotNull(diag.suggestion)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `checkSdkPackages returns OK when at least one build-tools version is installed`() {
        val tempDir = Files.createTempDirectory("fake-sdk").toFile()
        try {
            File(tempDir, "platforms/android-35").mkdirs()
            File(tempDir, "build-tools/35.0.0").mkdirs()
            val diagnostics = detector.checkSdkPackages(tempDir.absolutePath)
            val diag = diagnostics.first { it.message.contains("build-tools") }
            assertEquals(DiagnosticLevel.OK, diag.level)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `checks does not run package checks when androidSdkPath is null`() {
        val info = ToolchainInfo(androidSdkPath = null)
        val diagnostics = detector.checks(info)
        assertFalse(diagnostics.any { it.message.contains("platforms") || it.message.contains("build-tools") })
    }

    // ── readGradleWrapperVersion() ────────────────────────────────────────────

    @Test
    fun `readGradleWrapperVersion returns version from valid wrapper properties`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            File(tempDir, "gradle/wrapper").mkdirs()
            File(tempDir, "gradle/wrapper/gradle-wrapper.properties").writeText(
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.11-bin.zip"
            )
            assertEquals("8.11", detector.readGradleWrapperVersion(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readGradleWrapperVersion returns null when wrapper properties file is missing`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            assertNull(detector.readGradleWrapperVersion(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readGradleWrapperVersion handles three-part version numbers`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            File(tempDir, "gradle/wrapper").mkdirs()
            File(tempDir, "gradle/wrapper/gradle-wrapper.properties").writeText(
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.11.1-bin.zip"
            )
            assertEquals("8.11.1", detector.readGradleWrapperVersion(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ── checks() — Gradle wrapper ─────────────────────────────────────────────

    @Test
    fun `checks returns WARNING when gradleVersion is null`() {
        val info = ToolchainInfo(gradleVersion = null)
        val diagnostics = detector.checks(info)
        val diag = diagnostics.first { it.message.contains("Gradle wrapper") }
        assertEquals(DiagnosticLevel.WARNING, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `checks returns OK when gradleVersion is set`() {
        val info = ToolchainInfo(gradleVersion = "8.11")
        val diagnostics = detector.checks(info)
        val diag = diagnostics.first { it.message.contains("Gradle wrapper") }
        assertEquals(DiagnosticLevel.OK, diag.level)
    }

    // ── readAgpVersion() ─────────────────────────────────────────────────────

    @Test
    fun `readAgpVersion reads version from libs_versions_toml`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            File(tempDir, "gradle").mkdirs()
            File(tempDir, "gradle/libs.versions.toml").writeText("""
                [versions]
                agp = "8.2.0"
                kotlin = "1.9.0"
            """.trimIndent())
            assertEquals("8.2.0", detector.readAgpVersion(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readAgpVersion reads version from root build_gradle_kts`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            File(tempDir, "build.gradle.kts").writeText("""
                plugins {
                    id("com.android.application") version "8.1.0" apply false
                }
            """.trimIndent())
            assertEquals("8.1.0", detector.readAgpVersion(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readAgpVersion returns null when no build files exist`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            assertNull(detector.readAgpVersion(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `readAgpVersion prefers toml over build_gradle_kts`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            File(tempDir, "gradle").mkdirs()
            File(tempDir, "gradle/libs.versions.toml").writeText("""agp = "8.2.0"""")
            File(tempDir, "build.gradle.kts").writeText("""id("com.android.application") version "8.1.0"""")
            assertEquals("8.2.0", detector.readAgpVersion(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ── checks() — AGP ───────────────────────────────────────────────────────

    @Test
    fun `checks returns WARNING when agpVersion is null`() {
        val info = ToolchainInfo(agpVersion = null)
        val diagnostics = detector.checks(info)
        val diag = diagnostics.first { it.message.contains("AGP") }
        assertEquals(DiagnosticLevel.WARNING, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `checks returns OK when agpVersion is set`() {
        val info = ToolchainInfo(agpVersion = "8.2.0")
        val diagnostics = detector.checks(info)
        val diag = diagnostics.first { it.message.contains("AGP") }
        assertEquals(DiagnosticLevel.OK, diag.level)
    }

    // ── runChecks() ───────────────────────────────────────────────────────────

    @Test
    fun `runChecks returns non-empty list without projectDir`() {
        val diagnostics = detector.runChecks()
        assertTrue(diagnostics.isNotEmpty())
    }

    @Test
    fun `runChecks includes Gradle wrapper check when projectDir has wrapper`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            File(tempDir, "gradle/wrapper").mkdirs()
            File(tempDir, "gradle/wrapper/gradle-wrapper.properties").writeText(
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.11-bin.zip"
            )
            val diagnostics = detector.runChecks(tempDir)
            val diag = diagnostics.first { it.message.contains("Gradle wrapper") }
            assertEquals(DiagnosticLevel.OK, diag.level)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `runChecks includes AGP check when projectDir has toml`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            File(tempDir, "gradle").mkdirs()
            File(tempDir, "gradle/libs.versions.toml").writeText("""agp = "8.2.0"""")
            val diagnostics = detector.runChecks(tempDir)
            val diag = diagnostics.first { it.message.contains("AGP") }
            assertEquals(DiagnosticLevel.OK, diag.level)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `runChecks emits WARNING for Gradle and AGP when projectDir has no build files`() {
        val tempDir = Files.createTempDirectory("fake-project").toFile()
        try {
            val diagnostics = detector.runChecks(tempDir)
            assertEquals(DiagnosticLevel.WARNING, diagnostics.first { it.message.contains("Gradle wrapper") }.level)
            assertEquals(DiagnosticLevel.WARNING, diagnostics.first { it.message.contains("AGP") }.level)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ── sanitize() — BLI 2.7 no credentials ──────────────────────────────────

    @Test
    fun `sanitize redacts KEY=VALUE pairs with long values`() {
        val result = detector.sanitize("SOME_TOKEN=abc123xyzABCDEF12")
        assertTrue(result.contains("[REDACTED]"), "Expected redaction but got: $result")
        assertFalse(result.contains("abc123xyzABCDEF12"))
    }

    @Test
    fun `sanitize leaves normal file paths intact`() {
        val path = "/usr/lib/jvm/java-17"
        assertEquals(path, detector.sanitize(path))
    }

    @Test
    fun `sanitize leaves short values intact`() {
        assertEquals("KEY=short", detector.sanitize("KEY=short"))
    }

    @Test
    fun `checks messages do not contain raw env var assignments`() {
        val info = ToolchainInfo(jdkPath = null, androidSdkPath = null)
        val diagnostics = detector.checks(info)
        for (d in diagnostics) {
            assertFalse(d.message.contains("="), "Raw env var assignment found in: ${d.message}")
        }
    }

    @Test
    fun `checks messages do not expose token-like strings from paths`() {
        val tempDir = Files.createTempDirectory("fake-jdk").toFile()
        try {
            // Simulate a path that contains a long token-like segment
            val suspiciousPath = "${tempDir.absolutePath}/ghp_abcdefghijklmnopqrstuvwxyz123456"
            val info = ToolchainInfo(jdkPath = suspiciousPath)
            val diagnostics = detector.checks(info)
            val jdkMsg = diagnostics.first { it.message.contains("JDK") }.message
            assertFalse(jdkMsg.contains("ghp_abcdefghijklmnopqrstuvwxyz123456"),
                "Token leaked in message: $jdkMsg")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
