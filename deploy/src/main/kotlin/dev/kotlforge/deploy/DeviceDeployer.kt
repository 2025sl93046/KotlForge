package dev.kotlforge.deploy

import dev.kotlforge.engine.ProcessRunner
import dev.kotlforge.engine.DefaultProcessRunner
import java.io.File

data class DeployResult(
    val success: Boolean,
    val deviceSerial: String? = null,
    val message: String
)

class DeviceDeployer(
    private val processRunner: ProcessRunner = DefaultProcessRunner()
) {

    fun deploy(apkPath: String, deviceSerial: String? = null): DeployResult {
        val adb = AdbLocator.locate()
            ?: return DeployResult(
                success = false,
                message = "ADB not found. Install Android SDK and set ANDROID_HOME, or add adb to PATH."
            )

        val detector = DeviceDetector(adb, processRunner)
        val devices = detector.listDevices().filter { it.isOnline }

        if (devices.isEmpty()) {
            return DeployResult(
                success = false,
                message = "No devices connected. Connect a device via USB or pair over WiFi with: kotlforge deploy --pair <ip:port> <code>"
            )
        }

        val target = when {
            deviceSerial != null -> {
                devices.find { it.serial == deviceSerial }
                    ?: return DeployResult(success = false, message = "Device '$deviceSerial' not found. Connected: ${devices.map { it.serial }}")
            }
            devices.size == 1 -> devices.first()
            else -> return DeployResult(
                success = false,
                message = "Multiple devices connected. Specify one with --device: ${devices.map { it.serial }}"
            )
        }

        val installer = ApkInstaller(adb, processRunner)
        return installer.install(apkPath, target.serial).fold(
            onSuccess = { DeployResult(success = true, deviceSerial = target.serial, message = "Installed on ${target.serial}") },
            onFailure = { DeployResult(success = false, deviceSerial = target.serial, message = it.message ?: "Unknown error") }
        )
    }

    fun pair(ipPort: String, pairingCode: String): DeployResult {
        val adb = AdbLocator.locate()
            ?: return DeployResult(success = false, message = "ADB not found. Set ANDROID_HOME or add adb to PATH.")

        return WifiPairing(adb, processRunner).pair(ipPort, pairingCode).fold(
            onSuccess = { DeployResult(success = true, message = "Paired with $ipPort") },
            onFailure = { DeployResult(success = false, message = it.message ?: "Pairing failed") }
        )
    }

    fun connect(ipPort: String): DeployResult {
        val adb = AdbLocator.locate()
            ?: return DeployResult(success = false, message = "ADB not found. Set ANDROID_HOME or add adb to PATH.")

        return WifiConnector(adb, processRunner).connect(ipPort).fold(
            onSuccess = { DeployResult(success = true, message = "Connected to $ipPort") },
            onFailure = { DeployResult(success = false, message = it.message ?: "Connection failed") }
        )
    }
}
