package dev.kotlforge.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BuildEngineTest {

    @Test
    fun `build engine accepts BuildRequest and returns BuildResult`() {
        val engine = mockk<BuildEngine>()
        val request = BuildRequest(projectDir = "/tmp/my-app")
        val expected = BuildResult(success = true, exitCode = 0, durationMillis = 1000)

        every { engine.build(request) } returns expected

        val result = engine.build(request)

        assertEquals(expected, result)
        verify(exactly = 1) { engine.build(request) }
    }

    @Test
    fun `build engine returns failed result on non-zero exit code`() {
        val engine = mockk<BuildEngine>()
        val request = BuildRequest(projectDir = "/tmp/bad-app")
        val expected = BuildResult(
            success = false,
            exitCode = 1,
            durationMillis = 500,
            diagnostics = listOf(Diagnostic(DiagnosticLevel.ERROR, "Build failed", "Check Gradle output"))
        )

        every { engine.build(request) } returns expected

        val result = engine.build(request)

        assertFalse(result.success)
        assertEquals(1, result.exitCode)
        assertTrue(result.diagnostics.isNotEmpty())
    }

    @Test
    fun `build engine can be called with custom variant and tasks`() {
        val engine = mockk<BuildEngine>()
        val request = BuildRequest(
            projectDir = "/tmp/my-app",
            variant = "release",
            tasks = listOf("assembleRelease")
        )
        val expected = BuildResult(success = true, exitCode = 0, durationMillis = 2000)

        every { engine.build(request) } returns expected

        val result = engine.build(request)

        assertTrue(result.success)
        verify(exactly = 1) { engine.build(request) }
    }
}
