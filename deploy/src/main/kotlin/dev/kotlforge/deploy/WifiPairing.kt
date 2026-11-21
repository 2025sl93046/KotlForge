package dev.kotlforge.deploy

import dev.kotlforge.engine.ProcessRunner
import dev.kotlforge.engine.DefaultProcessRunner
import java.io.File

class WifiPairing(
    private val adb: File,
    private val processRunner: ProcessRunner = DefaultProcessRunner()
) {

    fun pair(ipPort: String, pairingCode: String): Result<Unit> {
        val result = processRunner.run(
            command = listOf(adb.absolutePath, "pair", ipPort, pairingCode),
            workingDir = adb.parentFile
        )
        return if (result.exitCode == 0) Result.success(Unit)
        else Result.failure(RuntimeException("Pairing failed: ${result.stderr.ifBlank { result.stdout }}"))
    }
}
