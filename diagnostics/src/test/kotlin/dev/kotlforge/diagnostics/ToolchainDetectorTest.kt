package dev.kotlforge.diagnostics

import dev.kotlforge.core.DiagnosticLevel
import dev.kotlforge.core.ToolchainInfo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Files

class ToolchainDetectorTest {

    private val detector = ToolchainDetector()

    // ── detect() ─────────────────────────────────────────────────────────────

    @Test
    fun `detect returns a ToolchainInfo with jdkPath from JAVA_HOME`() {
        val info = detector.detect()
        assertNotNull(info)
        assertTrue(info is ToolchainInfo)
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
}
