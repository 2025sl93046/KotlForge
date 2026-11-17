package dev.kotlforge.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BuildResultTest {

    @Test
    fun `success is false when exitCode is non-zero`() {
        val result = BuildResult(success = false, exitCode = 1, durationMillis = 100)
        assertFalse(result.success)
        assertEquals(1, result.exitCode)
    }

    @Test
    fun `success is true with exitCode zero and artifact`() {
        val artifact = BuildArtifact(ArtifactType.APK, "app/build/outputs/apk/debug/app-debug.apk", 2457600)
        val result = BuildResult(success = true, exitCode = 0, durationMillis = 18400, artifacts = listOf(artifact))
        assertTrue(result.success)
        assertEquals(1, result.artifacts.size)
        assertEquals(ArtifactType.APK, result.artifacts[0].type)
    }

    @Test
    fun `default artifacts and diagnostics are empty`() {
        val result = BuildResult(success = true, exitCode = 0, durationMillis = 500)
        assertTrue(result.artifacts.isEmpty())
        assertTrue(result.diagnostics.isEmpty())
    }

    @Test
    fun `durationMillis is preserved`() {
        val result = BuildResult(success = true, exitCode = 0, durationMillis = 18400)
        assertEquals(18400, result.durationMillis)
    }

    @Test
    fun `serializes to JSON and back`() {
        val result = BuildResult(
            success = true,
            exitCode = 0,
            durationMillis = 18400,
            artifacts = listOf(BuildArtifact(ArtifactType.APK, "app/build/outputs/apk/debug/app-debug.apk", 2457600)),
            diagnostics = listOf(Diagnostic(DiagnosticLevel.WARNING, "SDK platform missing", "Run sdkmanager"))
        )
        val json = Json.encodeToString(result)
        val decoded = Json.decodeFromString<BuildResult>(json)
        assertEquals(result, decoded)
    }
}

class BuildArtifactTest {

    @Test
    fun `APK artifact fields are preserved`() {
        val artifact = BuildArtifact(ArtifactType.APK, "app/build/outputs/apk/debug/app-debug.apk", 2457600)
        assertEquals(ArtifactType.APK, artifact.type)
        assertEquals("app/build/outputs/apk/debug/app-debug.apk", artifact.path)
        assertEquals(2457600, artifact.sizeBytes)
    }

    @Test
    fun `AAB artifact type is supported`() {
        val artifact = BuildArtifact(ArtifactType.AAB, "app/build/outputs/bundle/release/app-release.aab", 3000000)
        assertEquals(ArtifactType.AAB, artifact.type)
    }

    @Test
    fun `serializes to JSON and back`() {
        val artifact = BuildArtifact(ArtifactType.APK, "app/build/outputs/apk/debug/app-debug.apk", 2457600)
        val json = Json.encodeToString(artifact)
        val decoded = Json.decodeFromString<BuildArtifact>(json)
        assertEquals(artifact, decoded)
    }
}
