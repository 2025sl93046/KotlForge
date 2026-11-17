package dev.kotlforge.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiagnosticTest {

    @Test
    fun `OK level diagnostic has no suggestion by default`() {
        val d = Diagnostic(level = DiagnosticLevel.OK, message = "JDK detected")
        assertEquals(DiagnosticLevel.OK, d.level)
        assertNull(d.suggestion)
    }

    @Test
    fun `WARNING level diagnostic can have a suggestion`() {
        val d = Diagnostic(
            level = DiagnosticLevel.WARNING,
            message = "Android SDK platform 34 not installed",
            suggestion = "Run: sdkmanager \"platforms;android-34\""
        )
        assertEquals(DiagnosticLevel.WARNING, d.level)
        assertNotNull(d.suggestion)
    }

    @Test
    fun `ERROR level diagnostic must have a suggestion`() {
        val d = Diagnostic(
            level = DiagnosticLevel.ERROR,
            message = "JAVA_HOME is not set",
            suggestion = "Set JAVA_HOME to a valid JDK 17 installation"
        )
        assertEquals(DiagnosticLevel.ERROR, d.level)
        assertNotNull(d.suggestion)
        assertTrue(d.suggestion!!.isNotBlank())
    }

    @Test
    fun `all three diagnostic levels are supported`() {
        val levels = DiagnosticLevel.entries.map { it.name }
        assertTrue(levels.contains("OK"))
        assertTrue(levels.contains("WARNING"))
        assertTrue(levels.contains("ERROR"))
    }

    @Test
    fun `serializes to JSON and back`() {
        val d = Diagnostic(
            level = DiagnosticLevel.ERROR,
            message = "JAVA_HOME is not set",
            suggestion = "Set JAVA_HOME to a valid JDK 17 installation"
        )
        val json = Json.encodeToString(d)
        val decoded = Json.decodeFromString<Diagnostic>(json)
        assertEquals(d, decoded)
    }

    @Test
    fun `null suggestion serializes correctly`() {
        val d = Diagnostic(level = DiagnosticLevel.OK, message = "All good")
        val json = Json.encodeToString(d)
        val decoded = Json.decodeFromString<Diagnostic>(json)
        assertNull(decoded.suggestion)
    }
}
