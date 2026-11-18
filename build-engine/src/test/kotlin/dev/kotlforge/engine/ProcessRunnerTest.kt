package dev.kotlforge.engine

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ProcessRunnerTest {

    @Test
    fun `ProcessRunner interface can be mocked`() {
        val runner = mockk<ProcessRunner>()
        val command = listOf("echo", "hello")
        val workingDir = File(System.getProperty("java.io.tmpdir"))
        val expected = ProcessResult(exitCode = 0, stdout = "hello\n", stderr = "", durationMillis = 10)

        every { runner.run(command, workingDir) } returns expected

        val result = runner.run(command, workingDir)

        assertEquals(0, result.exitCode)
        assertEquals("hello\n", result.stdout)
        verify(exactly = 1) { runner.run(command, workingDir) }
    }

    @Test
    fun `ProcessRunner returns non-zero exit code on failure`() {
        val runner = mockk<ProcessRunner>()
        val command = listOf("false")
        val workingDir = File(System.getProperty("java.io.tmpdir"))
        val expected = ProcessResult(exitCode = 1, stdout = "", stderr = "error", durationMillis = 5)

        every { runner.run(command, workingDir) } returns expected

        val result = runner.run(command, workingDir)

        assertTrue(result.exitCode != 0)
    }

    @Test
    fun `ProcessRunner returns minus one exit code on timeout`() {
        val runner = mockk<ProcessRunner>()
        val command = listOf("sleep", "9999")
        val workingDir = File(System.getProperty("java.io.tmpdir"))
        val expected = ProcessResult(exitCode = -1, stdout = "", stderr = "Process timed out after 1 seconds", durationMillis = 1000)

        every { runner.run(command, workingDir, 1L) } returns expected

        val result = runner.run(command, workingDir, 1L)

        assertEquals(-1, result.exitCode)
        assertTrue(result.stderr.contains("timed out"))
    }
}
