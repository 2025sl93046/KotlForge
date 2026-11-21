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
    fun `findAab locates AAB at Gradle bundle output path`(@TempDir tempDir: File) {
        val aabDir = File(tempDir, "app/build/outputs/bundle/release").also { it.mkdirs() }
        val aab = File(aabDir, "app-release.aab").also { it.writeBytes(ByteArray(2048)) }
        val runner = GradleRunner()
        assertEquals(aab.absolutePath, runner.findAab(tempDir, "release"))
    }

    @Test
    fun `findAab returns null when no AAB is present`(@TempDir tempDir: File) {
        val runner = GradleRunner()
        assertNull(runner.findAab(tempDir, "debug"))
    }

    @Test
    fun `build includes AAB artifact when only AAB is produced`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }
        val aabDir = File(tempDir, "app/build/outputs/bundle/release").also { it.mkdirs() }
        File(aabDir, "app-release.aab").also { it.writeBytes(ByteArray(4096)) }

        val mockRunner = mockk<ProcessRunner>()
        every { mockRunner.run(any(), any(), any()) } returns
                ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 50)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "release", tasks = listOf("bundleRelease")))

        assertTrue(result.success)
        assertEquals(1, result.artifacts.size)
        assertEquals(dev.kotlforge.core.ArtifactType.AAB, result.artifacts.first().type)
        assertEquals(4096L, result.artifacts.first().sizeBytes)
    }

    @Test
    fun `build includes both APK and AAB when both are present`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }
        val apkDir = File(tempDir, "app/build/outputs/apk/debug").also { it.mkdirs() }
        File(apkDir, "app-debug.apk").also { it.writeBytes(ByteArray(1024)) }
        val aabDir = File(tempDir, "app/build/outputs/bundle/debug").also { it.mkdirs() }
        File(aabDir, "app-debug.aab").also { it.writeBytes(ByteArray(2048)) }

        val mockRunner = mockk<ProcessRunner>()
        every { mockRunner.run(any(), any(), any()) } returns
                ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 50)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "debug"))

        assertTrue(result.success)
        assertEquals(2, result.artifacts.size)
        assertTrue(result.artifacts.any { it.type == dev.kotlforge.core.ArtifactType.APK })
        assertTrue(result.artifacts.any { it.type == dev.kotlforge.core.ArtifactType.AAB })
    }

    @Test
    fun `build fails when AAB is empty zero bytes`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }
        val aabDir = File(tempDir, "app/build/outputs/bundle/release").also { it.mkdirs() }
        File(aabDir, "app-release.aab").also { it.createNewFile() }

        val mockRunner = mockk<ProcessRunner>()
        every { mockRunner.run(any(), any(), any()) } returns
                ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 50)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "release", tasks = listOf("bundleRelease")))

        assertFalse(result.success)
        assertTrue(result.artifacts.isEmpty())
    }

    @Test
    fun `build fails with no APK or AAB message when neither is produced`(@TempDir tempDir: File) {
        File(tempDir, "gradlew").also { it.createNewFile(); it.setExecutable(true) }

        val mockRunner = mockk<ProcessRunner>()
        every { mockRunner.run(any(), any(), any()) } returns
                ProcessResult(exitCode = 0, stdout = "", stderr = "", durationMillis = 50)

        val runner = GradleRunner(processRunner = mockRunner)
        val result = runner.build(BuildRequest(projectDir = tempDir.absolutePath, variant = "debug"))

        assertFalse(result.success)
        assertTrue(result.diagnostics.first().message.contains("No APK or AAB"))
    }
}
