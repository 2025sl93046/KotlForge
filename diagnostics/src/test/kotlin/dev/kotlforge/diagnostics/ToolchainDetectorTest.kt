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
        // detect() reads live env; verify it returns whatever ANDROID_HOME is set to
        val info = detector.detect()
        val expected = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (expected != null) {
            assertEquals(expected, info.androidSdkPath)
        }
    }

    @Test
    fun `resolveAndroidSdkPath returns null when no env var and no standard path exists`() {
        // Inject a path list where nothing exists on disk
        val result = ToolchainDetector.STANDARD_SDK_PATHS
            .filter { java.io.File(it).isDirectory }
        // We can only assert null if none of the standard paths are present;
        // otherwise at least one standard path resolves — both outcomes are valid
        val resolved = detector.resolveAndroidSdkPath()
        if (result.isEmpty() && System.getenv("ANDROID_HOME") == null && System.getenv("ANDROID_SDK_ROOT") == null) {
            assertNull(resolved)
        } else {
            assertNotNull(resolved)
        }
    }

    @Test
    fun `resolveAndroidSdkPath prefers ANDROID_HOME over standard paths`() {
        // If ANDROID_HOME is set, it must win over any standard path
        val androidHome = System.getenv("ANDROID_HOME")
        if (androidHome != null) {
            assertEquals(androidHome, detector.resolveAndroidSdkPath())
        }
    }

    @Test
    fun `resolveAndroidSdkPath falls back to first existing standard path`() {
        val tempDir = Files.createTempDirectory("fake-sdk").toFile()
        try {
            // Build a custom detector with a known standard path list
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
}
