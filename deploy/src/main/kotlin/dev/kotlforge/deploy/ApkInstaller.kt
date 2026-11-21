package dev.kotlforge.deploy

import dev.kotlforge.engine.ProcessRunner
import dev.kotlforge.engine.DefaultProcessRunner
import java.io.File

class ApkInstaller(
    private val adb: File,
    private val processRunner: ProcessRunner = DefaultProcessRunner()
) {

    fun install(apkPath: String, deviceSerial: String): Result<Unit> {
        val result = processRunner.run(
            command = listOf(adb.absolutePath, "-s", deviceSerial, "install", "-r", apkPath),
            workingDir = adb.parentFile
        )
        val output = result.stdout.trim()
        return if (result.exitCode == 0 && output.contains("Success", ignoreCase = true)) {
            Result.success(Unit)
        } else {
            val error = result.stderr.ifBlank { output }
            Result.failure(RuntimeException("Install failed on $deviceSerial: $error"))
        }
    }
}
