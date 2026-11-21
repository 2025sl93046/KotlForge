package dev.kotlforge.deploy

import dev.kotlforge.engine.ProcessResult
import dev.kotlforge.engine.ProcessRunner
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createFile

class DeviceDetectorTest {

    @TempDir
    lateinit var tempDir: Path

    private val processRunner = mockk<ProcessRunner>()

    @Test
    fun `returns online devices`() {
        val adb = tempDir.resolve("adb").createFile().toFile()
        every { processRunner.run(any(), any(), any()) } returns ProcessResult(
            exitCode = 0,
            stdout = "List of devices attached\nemulator-5554\tdevice\n192.168.1.5:5555\tdevice\n",
            stderr = "",
            durationMillis = 50
        )

        val devices = DeviceDetector(adb, processRunner).listDevices()

        assertEquals(2, devices.size)
        assertTrue(devices.all { it.isOnline })
        assertEquals("emulator-5554", devices[0].serial)
        assertEquals("192.168.1.5:5555", devices[1].serial)
    }

    @Test
    fun `filters out offline devices`() {
        val adb = tempDir.resolve("adb").createFile().toFile()
        every { processRunner.run(any(), any(), any()) } returns ProcessResult(
            exitCode = 0,
            stdout = "List of devices attached\nemulator-5554\toffline\n192.168.1.5:5555\tdevice\n",
            stderr = "",
            durationMillis = 50
        )

        val devices = DeviceDetector(adb, processRunner).listDevices()
            .filter { it.isOnline }

        assertEquals(1, devices.size)
        assertEquals("192.168.1.5:5555", devices[0].serial)
    }

    @Test
    fun `returns empty list when no devices`() {
        val adb = tempDir.resolve("adb").createFile().toFile()
        every { processRunner.run(any(), any(), any()) } returns ProcessResult(
            exitCode = 0,
            stdout = "List of devices attached\n",
            stderr = "",
            durationMillis = 50
        )

        val devices = DeviceDetector(adb, processRunner).listDevices()

        assertTrue(devices.isEmpty())
    }
}

class ApkInstallerTest {

    @TempDir
    lateinit var tempDir: Path

    private val processRunner = mockk<ProcessRunner>()

    @Test
    fun `returns success when install succeeds`() {
        val adb = tempDir.resolve("adb").createFile().toFile()
        every { processRunner.run(any(), any(), any()) } returns ProcessResult(
            exitCode = 0, stdout = "Performing Streamed Install\nSuccess", stderr = "", durationMillis = 100
        )

        val result = ApkInstaller(adb, processRunner).install("/tmp/app.apk", "emulator-5554")

        assertTrue(result.isSuccess)
        verify { processRunner.run(match { it.contains("-s") && it.contains("emulator-5554") }, any(), any()) }
    }

    @Test
    fun `returns failure when install fails`() {
        val adb = tempDir.resolve("adb").createFile().toFile()
        every { processRunner.run(any(), any(), any()) } returns ProcessResult(
            exitCode = 1, stdout = "INSTALL_FAILED_VERSION_DOWNGRADE", stderr = "", durationMillis = 100
        )

        val result = ApkInstaller(adb, processRunner).install("/tmp/app.apk", "emulator-5554")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("emulator-5554"))
    }
}

class WifiPairingTest {

    @TempDir
    lateinit var tempDir: Path

    private val processRunner = mockk<ProcessRunner>()

    @Test
    fun `returns success on successful pairing`() {
        val adb = tempDir.resolve("adb").createFile().toFile()
        every { processRunner.run(any(), any(), any()) } returns ProcessResult(
            exitCode = 0, stdout = "Successfully paired", stderr = "", durationMillis = 50
        )

        val result = WifiPairing(adb, processRunner).pair("192.168.1.5:37853", "123456")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure when pairing fails`() {
        val adb = tempDir.resolve("adb").createFile().toFile()
        every { processRunner.run(any(), any(), any()) } returns ProcessResult(
            exitCode = 1, stdout = "", stderr = "Failed to pair", durationMillis = 50
        )

        val result = WifiPairing(adb, processRunner).pair("192.168.1.5:37853", "wrongcode")

        assertTrue(result.isFailure)
    }
}

class WifiConnectorTest {

    @TempDir
    lateinit var tempDir: Path

    private val processRunner = mockk<ProcessRunner>()

    @Test
    fun `returns success on successful connection`() {
        val adb = tempDir.resolve("adb").createFile().toFile()
        every { processRunner.run(any(), any(), any()) } returns ProcessResult(
            exitCode = 0, stdout = "connected to 192.168.1.5:5555", stderr = "", durationMillis = 50
        )

        val result = WifiConnector(adb, processRunner).connect("192.168.1.5:5555")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure when connection refused`() {
        val adb = tempDir.resolve("adb").createFile().toFile()
        every { processRunner.run(any(), any(), any()) } returns ProcessResult(
            exitCode = 0, stdout = "failed to connect to 192.168.1.5:5555", stderr = "", durationMillis = 50
        )

        val result = WifiConnector(adb, processRunner).connect("192.168.1.5:5555")

        assertTrue(result.isFailure)
    }
}
