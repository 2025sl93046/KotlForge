package dev.kotlforge.watch

import dev.kotlforge.core.ArtifactType
import dev.kotlforge.core.BuildArtifact
import dev.kotlforge.core.BuildEngine
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.core.BuildResult
import dev.kotlforge.core.Diagnostic
import dev.kotlforge.core.DiagnosticLevel
import dev.kotlforge.deploy.DeployResult
import dev.kotlforge.deploy.DeviceDeployer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class WatchEngineTest {

    @TempDir
    lateinit var tempDir: Path

    private val buildEngine = mockk<BuildEngine>()
    private val deployer = mockk<DeviceDeployer>()

    private fun successResult(apkPath: String = "/tmp/app-debug.apk") = BuildResult(
        success = true,
        exitCode = 0,
        durationMillis = 500,
        artifacts = listOf(BuildArtifact(type = ArtifactType.APK, path = apkPath, sizeBytes = 1024))
    )

    private fun failResult() = BuildResult(
        success = false,
        exitCode = 1,
        durationMillis = 200,
        diagnostics = listOf(Diagnostic(level = DiagnosticLevel.ERROR, message = "compilation error", suggestion = null))
    )

    @Test
    fun `runCycle returns successful result on successful build`() {
        every { buildEngine.build(any()) } returns successResult()
        val engine = WatchEngine(buildEngine)

        val result = engine.runCycle(
            projectDir = tempDir.toFile(),
            variant = "debug",
            changedFiles = listOf(tempDir.resolve("Main.kt")),
            cycleNum = 1,
            deviceSerial = null
        )

        assertTrue(result.buildResult.success)
        assertEquals(1, result.cycle)
        assertEquals(1, result.changedFiles.size)
        assertNull(result.deployedDevice)
        verify { buildEngine.build(match { it.variant == "debug" }) }
    }

    @Test
    fun `runCycle returns failed result on build failure`() {
        every { buildEngine.build(any()) } returns failResult()
        val engine = WatchEngine(buildEngine)

        val result = engine.runCycle(
            projectDir = tempDir.toFile(),
            variant = "debug",
            changedFiles = listOf(tempDir.resolve("Main.kt")),
            cycleNum = 1,
            deviceSerial = null
        )

        assertFalse(result.buildResult.success)
        assertNull(result.deployedDevice)
    }

    @Test
    fun `runCycle deploys APK when deployer is set and build succeeds`() {
        every { buildEngine.build(any()) } returns successResult("/tmp/app.apk")
        every { deployer.deploy("/tmp/app.apk", null) } returns DeployResult(
            success = true, deviceSerial = "emulator-5554", message = "Installed"
        )
        val engine = WatchEngine(buildEngine, deployer)

        val result = engine.runCycle(
            projectDir = tempDir.toFile(),
            variant = "debug",
            changedFiles = listOf(tempDir.resolve("Main.kt")),
            cycleNum = 1,
            deviceSerial = null
        )

        assertTrue(result.buildResult.success)
        assertEquals("emulator-5554", result.deployedDevice)
        verify { deployer.deploy("/tmp/app.apk", null) }
    }

    @Test
    fun `runCycle does not deploy when build fails`() {
        every { buildEngine.build(any()) } returns failResult()
        val engine = WatchEngine(buildEngine, deployer)

        val result = engine.runCycle(
            projectDir = tempDir.toFile(),
            variant = "debug",
            changedFiles = listOf(tempDir.resolve("Main.kt")),
            cycleNum = 1,
            deviceSerial = null
        )

        assertFalse(result.buildResult.success)
        assertNull(result.deployedDevice)
        verify(exactly = 0) { deployer.deploy(any(), any()) }
    }

    @Test
    fun `runCycle passes specific device serial to deployer`() {
        every { buildEngine.build(any()) } returns successResult("/tmp/app.apk")
        every { deployer.deploy("/tmp/app.apk", "192.168.1.5:5555") } returns DeployResult(
            success = true, deviceSerial = "192.168.1.5:5555", message = "Installed"
        )
        val engine = WatchEngine(buildEngine, deployer)

        val result = engine.runCycle(
            projectDir = tempDir.toFile(),
            variant = "release",
            changedFiles = listOf(tempDir.resolve("Main.kt")),
            cycleNum = 2,
            deviceSerial = "192.168.1.5:5555"
        )

        assertEquals("192.168.1.5:5555", result.deployedDevice)
        verify { buildEngine.build(match { it.variant == "release" }) }
        verify { deployer.deploy("/tmp/app.apk", "192.168.1.5:5555") }
    }

    @Test
    fun `runCycle tracks cycle number correctly`() {
        every { buildEngine.build(any()) } returns successResult()
        val engine = WatchEngine(buildEngine)

        val result1 = engine.runCycle(tempDir.toFile(), "debug", emptyList(), 1, null)
        val result2 = engine.runCycle(tempDir.toFile(), "debug", emptyList(), 2, null)
        val result3 = engine.runCycle(tempDir.toFile(), "debug", emptyList(), 3, null)

        assertEquals(1, result1.cycle)
        assertEquals(2, result2.cycle)
        assertEquals(3, result3.cycle)
    }

    @Test
    fun `runCycle reports all changed files`() {
        every { buildEngine.build(any()) } returns successResult()
        val engine = WatchEngine(buildEngine)
        val files = (1..7).map { tempDir.resolve("File$it.kt") }

        val result = engine.runCycle(tempDir.toFile(), "debug", files, 1, null)

        assertEquals(7, result.changedFiles.size)
    }
}
