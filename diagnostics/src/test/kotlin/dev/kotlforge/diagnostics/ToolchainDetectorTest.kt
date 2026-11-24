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
    fun `detect returns jdkPath from injected env`() {
        val d = ToolchainDetector(env = { name -> mapOf("JAVA_HOME" to "/injected/jdk")[name] })
        assertEquals("/injected/jdk", d.detect().jdkPath)
    }

    @Test
    fun `detect returns null jdkPath when JAVA_HOME not set`() {
        val d = ToolchainDetector(env = { _ -> null })
        assertNull(d.detect().jdkPath)
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
    fun `resolveAndroidSdkPath returns injected ANDROID_HOME`() {
        val d = ToolchainDetector(env = { name -> mapOf("ANDROID_HOME" to "/injected/sdk")[name] })
        assertEquals("/injected/sdk", d.resolveAndroidSdkPath())
    }

    @Test
    fun `resolveAndroidSdkPath returns injected ANDROID_SDK_ROOT when ANDROID_HOME absent`() {
        val d = ToolchainDetector(env = { name ->
            mapOf("ANDROID_SDK_ROOT" to "/injected/sdk-root")[name]
        })
        assertEquals("/injected/sdk-root", d.resolveAndroidSdkPath())
    }

    @Test
    fun `resolveAndroidSdkPath prefers ANDROID_HOME over ANDROID_SDK_ROOT`() {
        val d = ToolchainDetector(env = { name ->
            mapOf("ANDROID_HOME" to "/home-sdk", "ANDROID_SDK_ROOT" to "/root-sdk")[name]
        })
        assertEquals("/home-sdk", d.resolveAndroidSdkPath())
    }

    @Test
    fun `resolveAndroidSdkPath falls back to first existing standard path when no env var`() {
        // When isDirectory always returns true, the first STANDARD_SDK_PATH is returned.
        val d = ToolchainDetector(
            env = { _ -> null },
            isDirectory = { _ -> true }
        )
        assertNotNull(d.resolveAndroidSdkPath())
    }

    @Test
    fun `resolveAndroidSdkPath returns null when no env var and no standard path exists`() {
        val d = ToolchainDetector(
            env = { _ -> null },
            isDirectory = { _ -> false }
        )
        assertNull(d.resolveAndroidSdkPath())
    }

    // ── BLI 2.11: Windows SDK path discovery ─────────────────────────────────

    @Test
    fun `expandWindowsTemplate expands LOCALAPPDATA token`() {
        val d = ToolchainDetector(env = { name -> if (name == "LOCALAPPDATA") "C:\\Users\\Dev\\AppData\\Local" else null })
        assertEquals(
            "C:\\Users\\Dev\\AppData\\Local\\Android\\Sdk",
            d.expandWindowsTemplate("%LOCALAPPDATA%\\Android\\Sdk")
        )
    }

    @Test
    fun `expandWindowsTemplate expands USERPROFILE token`() {
        val d = ToolchainDetector(env = { name -> if (name == "USERPROFILE") "C:\\Users\\Dev" else null })
        assertEquals(
            "C:\\Users\\Dev\\Android\\Sdk",
            d.expandWindowsTemplate("%USERPROFILE%\\Android\\Sdk")
        )
    }

    @Test
    fun `expandWindowsTemplate returns null when env var is not set`() {
        val d = ToolchainDetector(env = { _ -> null })
        assertNull(d.expandWindowsTemplate("%LOCALAPPDATA%\\Android\\Sdk"))
    }

    @Test
    fun `resolveAndroidSdkPath finds SDK via LOCALAPPDATA when no explicit env var`() {
        val fakeLocal = "C:\\Users\\Dev\\AppData\\Local"
        val expectedPath = "$fakeLocal\\Android\\Sdk"
        val d = ToolchainDetector(
            env = { name -> if (name == "LOCALAPPDATA") fakeLocal else null },
            isDirectory = { path -> path == expectedPath }
        )
        assertEquals(expectedPath, d.resolveAndroidSdkPath())
    }

    @Test
    fun `resolveAndroidSdkPath finds SDK via USERPROFILE when LOCALAPPDATA path absent`() {
        val fakeProfile = "C:\\Users\\Dev"
        val expectedPath = "$fakeProfile\\Android\\Sdk"
        val d = ToolchainDetector(
            env = { name -> if (name == "USERPROFILE") fakeProfile else null },
            isDirectory = { path -> path == expectedPath }
        )
        assertEquals(expectedPath, d.resolveAndroidSdkPath())
    }

    @Test
    fun `resolveAndroidSdkPath prefers ANDROID_HOME over Windows fallback paths`() {
        val d = ToolchainDetector(
            env = { name -> when (name) {
                "ANDROID_HOME"  -> "D:\\CustomSdk"
                "LOCALAPPDATA"  -> "C:\\Users\\Dev\\AppData\\Local"
                else            -> null
            }},
            isDirectory = { _ -> true }
        )
        assertEquals("D:\\CustomSdk", d.resolveAndroidSdkPath())
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

    // ── detect() with injected env — full fixture ─────────────────────────────

    @Test
    fun `detect uses injected env for both JAVA_HOME and ANDROID_HOME`() {
        val fakeEnv = mapOf(
            "JAVA_HOME" to "/fake/jdk",
            "ANDROID_HOME" to "/fake/sdk"
        )
        val d = ToolchainDetector(env = { name -> fakeEnv[name] })
        val info = d.detect()
        assertEquals("/fake/jdk", info.jdkPath)
        assertEquals("/fake/sdk", info.androidSdkPath)
    }

    @Test
    fun `detect returns null paths when env is empty`() {
        val d = ToolchainDetector(
            env = { _ -> null },
            isDirectory = { _ -> false }
        )
        val info = d.detect()
        assertNull(info.jdkPath)
        assertNull(info.androidSdkPath)
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

    // ── BLI 2.9: negative-path fixture tests ─────────────────────────────────

    @Test
    fun `runChecks reports ERROR for JDK when JAVA_HOME is not set`() {
        val d = ToolchainDetector(
            env = { _ -> null },
            isDirectory = { _ -> false }
        )
        val diagnostics = d.runChecks()
        val diag = diagnostics.first { it.message.contains("JDK") }
        assertEquals(DiagnosticLevel.ERROR, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `runChecks reports ERROR for JDK when JAVA_HOME points to nonexistent path`() {
        val d = ToolchainDetector(
            env = { name -> if (name == "JAVA_HOME") "/nonexistent/jdk" else null },
            isDirectory = { _ -> false }
        )
        val diagnostics = d.runChecks()
        val diag = diagnostics.first { it.message.contains("JDK") }
        assertEquals(DiagnosticLevel.ERROR, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `runChecks reports ERROR for Android SDK when ANDROID_HOME is not set`() {
        val d = ToolchainDetector(
            env = { _ -> null },
            isDirectory = { _ -> false }
        )
        val diagnostics = d.runChecks()
        val diag = diagnostics.first { it.message.contains("Android SDK") }
        assertEquals(DiagnosticLevel.ERROR, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `runChecks reports ERROR for Android SDK when ANDROID_HOME points to nonexistent path`() {
        val d = ToolchainDetector(
            env = { name -> if (name == "ANDROID_HOME") "/nonexistent/sdk" else null },
            isDirectory = { _ -> false }
        )
        val diagnostics = d.runChecks()
        val diag = diagnostics.first { it.message.contains("Android SDK") }
        assertEquals(DiagnosticLevel.ERROR, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `runChecks reports WARNING for Gradle wrapper when wrapper properties file is missing`() {
        val tempDir = Files.createTempDirectory("fake-project-no-wrapper").toFile()
        try {
            val d = ToolchainDetector(env = { _ -> null }, isDirectory = { _ -> false })
            val diagnostics = d.runChecks(tempDir)
            val diag = diagnostics.first { it.message.contains("Gradle wrapper") }
            assertEquals(DiagnosticLevel.WARNING, diag.level)
            assertNotNull(diag.suggestion)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `runChecks reports WARNING for AGP when no build files exist`() {
        val tempDir = Files.createTempDirectory("fake-project-no-agp").toFile()
        try {
            val d = ToolchainDetector(env = { _ -> null }, isDirectory = { _ -> false })
            val diagnostics = d.runChecks(tempDir)
            val diag = diagnostics.first { it.message.contains("AGP") }
            assertEquals(DiagnosticLevel.WARNING, diag.level)
            assertNotNull(diag.suggestion)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `runChecks all ERROR and WARNING when env is empty and no project files exist`() {
        val tempDir = Files.createTempDirectory("fake-project-empty").toFile()
        try {
            val d = ToolchainDetector(env = { _ -> null }, isDirectory = { _ -> false })
            val diagnostics = d.runChecks(tempDir)
            assertTrue(diagnostics.any { it.level == DiagnosticLevel.ERROR && it.message.contains("JDK") })
            assertTrue(diagnostics.any { it.level == DiagnosticLevel.ERROR && it.message.contains("Android SDK") })
            assertTrue(diagnostics.any { it.level == DiagnosticLevel.WARNING && it.message.contains("Gradle wrapper") })
            assertTrue(diagnostics.any { it.level == DiagnosticLevel.WARNING && it.message.contains("AGP") })
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ── BLI 2.12: compatibility warnings ─────────────────────────────────────

    @Test
    fun `checkCompatibility returns empty list when no versions are set`() {
        val info = ToolchainInfo()
        assertTrue(detector.checkCompatibility(info).isEmpty())
    }

    @Test
    fun `checkCompatibility returns WARNING for JDK 25 with Gradle 8`() {
        val info = ToolchainInfo(jdkVersion = "25", gradleVersion = "8.11")
        val diag = detector.checkCompatibility(info).first { it.message.contains("JDK") }
        assertEquals(DiagnosticLevel.WARNING, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `checkCompatibility returns no warning for JDK 21 with Gradle 8`() {
        val info = ToolchainInfo(jdkVersion = "21", gradleVersion = "8.11")
        assertFalse(detector.checkCompatibility(info).any { it.message.contains("JDK") && it.message.contains("Gradle") })
    }

    @Test
    fun `checkCompatibility returns WARNING for AGP 8 with Gradle 7`() {
        val info = ToolchainInfo(agpVersion = "8.1.0", gradleVersion = "7.6")
        val diag = detector.checkCompatibility(info).first { it.message.contains("AGP") }
        assertEquals(DiagnosticLevel.WARNING, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `checkCompatibility returns no warning for AGP 8 with Gradle 8`() {
        val info = ToolchainInfo(agpVersion = "8.1.0", gradleVersion = "8.11")
        assertFalse(detector.checkCompatibility(info).any { it.message.contains("AGP") })
    }

    @Test
    fun `checkCompatibility returns WARNING for AGP 7 with Gradle 6`() {
        val info = ToolchainInfo(agpVersion = "7.4.0", gradleVersion = "6.9")
        val diag = detector.checkCompatibility(info).first { it.message.contains("AGP") }
        assertEquals(DiagnosticLevel.WARNING, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `checkCompatibility returns WARNING for AGP 8_2 with JDK 11`() {
        val info = ToolchainInfo(agpVersion = "8.2.0", jdkVersion = "11")
        val diag = detector.checkCompatibility(info).first { it.message.contains("JDK 17") }
        assertEquals(DiagnosticLevel.WARNING, diag.level)
        assertNotNull(diag.suggestion)
    }

    @Test
    fun `checkCompatibility returns no warning for AGP 8_2 with JDK 17`() {
        val info = ToolchainInfo(agpVersion = "8.2.0", jdkVersion = "17")
        assertFalse(detector.checkCompatibility(info).any { it.message.contains("JDK 17") })
    }

    @Test
    fun `checks includes compatibility warnings in output`() {
        val info = ToolchainInfo(jdkVersion = "25", gradleVersion = "8.11")
        val diags = detector.checks(info)
        assertTrue(diags.any { it.level == DiagnosticLevel.WARNING && it.message.contains("JDK") && it.message.contains("Gradle") })
    }
}
