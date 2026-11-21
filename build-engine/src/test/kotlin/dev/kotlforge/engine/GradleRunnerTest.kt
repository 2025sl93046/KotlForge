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
        val result = runner.build(request)
        assertFalse(result.success)
        assertEquals(3, result.exitCode)
        assertTrue(result.diagnostics.first().message.contains("not found"))
    }

    @Test
    fun `returns failed result with chmod suggestion when gradlew is not executable`(@TempDir tempDir: File) {
        val wrapper = File(tempDir, "gradlew").also { it.createNewFile() }
        wrapper.setExecutable(false)
        val runner = GradleRunner()
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath))
        assertFalse(result.success)
        assertEquals(3, result.exitCode)
        val suggestion = result.diagnostics.first().suggestion ?: ""
        assertTrue(suggestion.contains("chmod"))
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
    fun `build delegates to ProcessRunner with correct command`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }

        val mockRunner = mockk<ProcessRunner>()
        every {
            mockRunner.run(any(), any(), any())
        } returns ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 100)

        val runner = GradleRunner(processRunner = mockRunner)
        runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "debug"))

        verify {
            mockRunner.run(
                command = match { it.contains("assembleDebug") },
                workingDir = tempDir,
                timeoutSeconds = any()
            )
        }
    }

    @Test
    fun `build returns failed result when ProcessRunner exits non-zero`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }

        val mockRunner = mockk<ProcessRunner>()
        every { mockRunner.run(any(), any(), any()) } returns
                ProcessResult(exitCode = 1, stdout = "", stderr = "error", durationMillis = 100)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "debug"))

        assertFalse(result.success)
        assertEquals(1, result.exitCode)
        val suggestion = result.diagnostics.first().suggestion ?: ""
        assertTrue(suggestion.contains("kotlforge-build.log"))
        assertTrue(suggestion.contains(tempDir.absolutePath))
    }

    @Test
    fun `findApk locates APK at Gradle standard output path`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }
        val apkDir = File(tempDir, "app/build/outputs/apk/debug").also { it.mkdirs() }
        val apk = File(apkDir, "app-debug.apk").also { it.writeBytes(ByteArray(1024)) }

        val mockRunner = mockk<ProcessRunner>()
        every { mockRunner.run(any(), any(), any()) } returns
                ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 50)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "debug"))

        assertTrue(result.success)
        assertEquals(apk.absolutePath, result.artifacts.first().path)
        assertEquals(1024L, result.artifacts.first().sizeBytes)
    }

    @Test
    fun `build returns failed result when APK is empty (0 bytes)`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }
        val apkDir = File(tempDir, "app/build/outputs/apk/debug").also { it.mkdirs() }
        File(apkDir, "app-debug.apk").also { it.createNewFile() }

        val mockRunner = mockk<ProcessRunner>()
        every { mockRunner.run(any(), any(), any()) } returns
                ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 50)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "debug"))

        assertFalse(result.success)
        assertTrue(result.artifacts.isEmpty())
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
    fun `build returns failed BuildResult on non-zero exit code`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }

        val mockRunner = mockk<ProcessRunner>()
        every {
            mockRunner.run(any(), any(), any())
        } returns ProcessResult(exitCode = 1, stdout = "", stderr = "FAILURE", durationMillis = 200)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath))

        assertFalse(result.success)
        assertEquals(1, result.exitCode)
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

    @Test
    fun `findApks discovers APK in app module`(@TempDir tempDir: File) {
        val apkDir = File(tempDir, "app/build/outputs/apk/debug").also { it.mkdirs() }
        val apk = File(apkDir, "app-debug.apk").also { it.writeBytes(ByteArray(1024)) }
        val runner = GradleRunner()
        val found = runner.findApks(tempDir, "debug")
        assertEquals(listOf(apk.absolutePath), found)
    }

    @Test
    fun `findApks discovers APKs across multiple app modules`(@TempDir tempDir: File) {
        val apk1Dir = File(tempDir, "app/build/outputs/apk/debug").also { it.mkdirs() }
        val apk1 = File(apk1Dir, "app-debug.apk").also { it.writeBytes(ByteArray(1024)) }
        val apk2Dir = File(tempDir, "feature-login/build/outputs/apk/debug").also { it.mkdirs() }
        val apk2 = File(apk2Dir, "feature-login-debug.apk").also { it.writeBytes(ByteArray(2048)) }

        val runner = GradleRunner()
        val found = runner.findApks(tempDir, "debug")

        assertEquals(2, found.size)
        assertTrue(found.contains(apk1.absolutePath))
        assertTrue(found.contains(apk2.absolutePath))
    }

    @Test
    fun `findApks returns empty list when no APKs present`(@TempDir tempDir: File) {
        val runner = GradleRunner()
        assertTrue(runner.findApks(tempDir, "debug").isEmpty())
    }

    @Test
    fun `build returns multiple APK artifacts for multi-module project`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }
        val apk1Dir = File(tempDir, "app/build/outputs/apk/debug").also { it.mkdirs() }
        File(apk1Dir, "app-debug.apk").also { it.writeBytes(ByteArray(1024)) }
        val apk2Dir = File(tempDir, "wear/build/outputs/apk/debug").also { it.mkdirs() }
        File(apk2Dir, "wear-debug.apk").also { it.writeBytes(ByteArray(2048)) }

        val mockRunner = mockk<ProcessRunner>()
        every { mockRunner.run(any(), any(), any()) } returns
                ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 50)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "debug"))

        assertTrue(result.success)
        assertEquals(2, result.artifacts.size)
        assertTrue(result.artifacts.sumOf { it.sizeBytes } == 3072L)
    }

    @Test
    fun `build fails when all discovered APKs are empty zero bytes`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }
        val apkDir = File(tempDir, "app/build/outputs/apk/debug").also { it.mkdirs() }
        File(apkDir, "app-debug.apk").also { it.createNewFile() }

        val mockRunner = mockk<ProcessRunner>()
        every { mockRunner.run(any(), any(), any()) } returns
                ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 50)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "debug"))

        assertFalse(result.success)
        assertTrue(result.artifacts.isEmpty())
    }
}
