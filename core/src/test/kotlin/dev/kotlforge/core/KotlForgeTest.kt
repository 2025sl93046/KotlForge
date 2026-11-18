package dev.kotlforge.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KotlForgeTest {

    private val engine = mockk<BuildEngine>()
    private val kotlForge = KotlForge(engine)

    @Test
    fun `delegates valid request to engine`() {
        val request = BuildRequest(projectDir = "/tmp/my-app")
        val expected = BuildResult(success = true, exitCode = 0, durationMillis = 1000)
        every { engine.build(request) } returns expected

        val result = kotlForge.build(request)

        assertEquals(expected, result)
        verify(exactly = 1) { engine.build(request) }
    }

    @Test
    fun `returns error result for blank projectDir`() {
        val request = BuildRequest(projectDir = "  ")

        val result = kotlForge.build(request)

        assertFalse(result.success)
        assertEquals(2, result.exitCode)
        assertTrue(result.diagnostics.any { it.message.contains("projectDir") })
        verify(exactly = 0) { engine.build(any()) }
    }

    @Test
    fun `returns error result for blank variant`() {
        val request = BuildRequest(projectDir = "/tmp/my-app", variant = "")

        val result = kotlForge.build(request)

        assertFalse(result.success)
        assertEquals(2, result.exitCode)
        assertTrue(result.diagnostics.any { it.message.contains("variant") })
        verify(exactly = 0) { engine.build(any()) }
    }

    @Test
    fun `all validation errors have ERROR level and suggestion`() {
        val request = BuildRequest(projectDir = "", variant = "")

        val result = kotlForge.build(request)

        assertTrue(result.diagnostics.isNotEmpty())
        result.diagnostics.forEach {
            assertEquals(DiagnosticLevel.ERROR, it.level)
            assertNotNull(it.suggestion)
        }
    }

    @Test
    fun `passes through engine result on success`() {
        val request = BuildRequest(projectDir = "/tmp/my-app", variant = "release")
        val artifact = BuildArtifact(ArtifactType.APK, "app/build/outputs/apk/release/app-release.apk", 3000000)
        val expected = BuildResult(success = true, exitCode = 0, durationMillis = 5000, artifacts = listOf(artifact))
        every { engine.build(request) } returns expected

        val result = kotlForge.build(request)

        assertTrue(result.success)
        assertEquals(1, result.artifacts.size)
        assertEquals(ArtifactType.APK, result.artifacts[0].type)
    }
}
