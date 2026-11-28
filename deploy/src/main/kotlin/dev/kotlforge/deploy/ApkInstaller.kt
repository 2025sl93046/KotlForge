package dev.kotlforge.deploy

import dev.kotlforge.engine.ProcessRunner
import dev.kotlforge.engine.DefaultProcessRunner
import java.io.File

enum class InstallMode { STANDARD, INCREMENTAL }

class ApkInstaller(
    private val adb: File,
    private val processRunner: ProcessRunner = DefaultProcessRunner()
) {

    fun install(apkPath: String, deviceSerial: String, mode: InstallMode = InstallMode.STANDARD): Result<Unit> {
        return if (mode == InstallMode.INCREMENTAL) {
            installIncremental(apkPath, deviceSerial)
        } else {
            installStandard(apkPath, deviceSerial)
        }
    }

    private fun installStandard(apkPath: String, deviceSerial: String): Result<Unit> {
        val result = processRunner.run(
            command = listOf(adb.absolutePath, "-s", deviceSerial, "install", "-r", "-d", "-g", apkPath),
            workingDir = adb.parentFile
        )
        return parseInstallResult(result.exitCode, result.stdout, result.stderr, deviceSerial)
    }

    private fun installIncremental(apkPath: String, deviceSerial: String): Result<Unit> {
        // adb install --incremental streams only changed APK blocks (Android 11+, requires FSVERITY)
        val result = processRunner.run(
            command = listOf(adb.absolutePath, "-s", deviceSerial, "install", "--incremental", "-r", "-d", "-g", apkPath),
            workingDir = adb.parentFile
        )
        // adb prints "device does not support incremental" or exits non-zero when unsupported — fall back
        val unsupported = result.exitCode != 0 &&
            (result.stderr.contains("incremental", ignoreCase = true) ||
             result.stdout.contains("incremental", ignoreCase = true))
        if (unsupported) {
            println("[install] Device does not support --incremental (Android 11+ required), falling back to standard install")
            return installStandard(apkPath, deviceSerial)
        }
        return parseInstallResult(result.exitCode, result.stdout, result.stderr, deviceSerial)
    }

    private fun parseInstallResult(exitCode: Int, stdout: String, stderr: String, deviceSerial: String): Result<Unit> {
        val output = stdout.trim()
        return if (exitCode == 0 && output.contains("Success", ignoreCase = true)) {
            Result.success(Unit)
        } else {
            val error = stderr.ifBlank { output }
            Result.failure(RuntimeException("Install failed on $deviceSerial: $error"))
        }
    }
}
