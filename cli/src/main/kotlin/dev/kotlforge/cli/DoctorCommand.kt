package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import dev.kotlforge.diagnostics.ToolchainDetector

class DoctorCommand : CliktCommand(
    name = "doctor",
    help = "Check toolchain requirements (JDK, Android SDK)"
) {
    override fun run() {
        val detector = ToolchainDetector()
        val info = detector.detect()
        val errors = detector.validate(info)

        echo("KotlForge Doctor")
        echo("  JDK:         ${info.jdkPath ?: "NOT FOUND"}")
        echo("  Android SDK: ${info.androidSdkPath ?: "NOT FOUND"}")

        if (errors.isEmpty()) {
            echo("  Status: OK")
        } else {
            errors.forEach { echo("  ERROR: $it") }
        }
    }
}
