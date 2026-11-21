package dev.kotlforge.deploy

import dev.kotlforge.engine.ProcessRunner
import dev.kotlforge.engine.DefaultProcessRunner
import java.io.File

class DeviceDetector(
    private val adb: File,
    private val processRunner: ProcessRunner = DefaultProcessRunner()
) {

    fun listDevices(): List<ConnectedDevice> {
        val result = processRunner.run(
            command = listOf(adb.absolutePath, "devices"),
            workingDir = adb.parentFile
        )
        return result.stdout.lines()
            .drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.trim().split("\t")
                if (parts.size == 2) ConnectedDevice(serial = parts[0], state = parts[1]) else null
            }
    }
}
