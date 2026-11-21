package dev.kotlforge.deploy

import dev.kotlforge.engine.ProcessRunner
import dev.kotlforge.engine.DefaultProcessRunner
import java.io.File

class WifiConnector(
    private val adb: File,
    private val processRunner: ProcessRunner = DefaultProcessRunner()
) {

    fun connect(ipPort: String): Result<Unit> {
        val result = processRunner.run(
            command = listOf(adb.absolutePath, "connect", ipPort),
            workingDir = adb.parentFile
        )
        val output = result.stdout.trim()
        return if (result.exitCode == 0 && !output.contains("failed", ignoreCase = true)) {
            Result.success(Unit)
        } else {
            Result.failure(RuntimeException("Connection failed: $output"))
        }
    }
}
