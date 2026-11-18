package dev.kotlforge.engine

import dev.kotlforge.core.BuildRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GradleRunnerTest {

    @Test
    fun `returns failed result when gradlew is missing`() {
        val runner = GradleRunner()
        val request = BuildRequest(projectDir = System.getProperty("java.io.tmpdir") + "/nonexistent-project-12345")
        val result = runner.run(request)
        assertFalse(result.success)
        assertEquals(3, result.exitCode)
    }

    @Test
    fun `locateWrapper returns null when wrapper is absent`(@TempDir tempDir: File) {
        val runner = GradleRunner()
        assertNull(runner.locateWrapper(tempDir))
    }

    @Test
    fun `locateWrapper returns file when gradlew exists`(@TempDir tempDir: File) {
        val wrapper = File(tempDir, "gradlew").also { it.createNewFile() }
        val runner = GradleRunner()
        val located = runner.locateWrapper(tempDir)
        assertNotNull(located)
        assertEquals(wrapper.absolutePath, located!!.absolutePath)
    }

    @Test
    fun `run delegates to ProcessRunner with correct command`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile() }

        val mockRunner = mockk<ProcessRunner>()
        every {
            mockRunner.run(any(), any(), any())
        } returns ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 100)

        val runner = GradleRunner(processRunner = mockRunner)
        runner.run(BuildRequest(projectDir = tempDir.absolutePath, variant = "debug"))

        verify {
            mockRunner.run(
                command = match { it.contains("assembleDebug") },
                workingDir = tempDir,
                timeoutSeconds = any()
            )
        }
    }

    @Test
    fun `writeRawLog writes stdout and stderr to kotlforge-build log`(@TempDir tempDir: File) {
        val runner = GradleRunner()
        runner.writeRawLog(tempDir, "output line", "error line")
        val log = File(tempDir, "kotlforge-build.log").readText()
        assertTrue(log.contains("output line"))
        assertTrue(log.contains("error line"))
    }

    @Test
    fun `resolveGradleTasks returns assembleDebug for debug variant`() {
        val runner = GradleRunner()
        assertEquals(listOf("assembleDebug"), runner.resolveGradleTasks(BuildRequest(projectDir = ".", variant = "debug")))
    }

    @Test
    fun `resolveGradleTasks returns assembleRelease for release variant`() {
        val runner = GradleRunner()
        assertEquals(listOf("assembleRelease"), runner.resolveGradleTasks(BuildRequest(projectDir = ".", variant = "release")))
    }

    @Test
    fun `resolveGradleTasks uses explicit tasks when provided`() {
        val runner = GradleRunner()
        val tasks = listOf("bundleRelease", "test")
        assertEquals(tasks, runner.resolveGradleTasks(BuildRequest(projectDir = ".", tasks = tasks)))
    }
}

