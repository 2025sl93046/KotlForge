package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.deploy.DeviceDeployer
import dev.kotlforge.engine.GradleRunner

class DeployCommand : CliktCommand(
    name = "deploy",
    help = "Build and deploy an APK to a connected Android device over WiFi"
) {
    private val projectPath by option("--project", "-p", help = "Path to the Android project").default(".")
    private val variant by option("--variant", "-v", help = "Build variant (debug/release)").default("debug")
    private val deviceSerial by option("--device", "-d", help = "Target device serial (auto-selected if only one connected)")
    private val pairAddress by option("--pair", help = "Pair with a device: --pair <ip:port> --code <pairing-code>")
    private val pairingCode by option("--code", help = "Pairing code for WiFi pairing")
    private val connectAddress by option("--connect", help = "Connect to a paired device: --connect <ip:port>")

    override fun run() {
        val deployer = DeviceDeployer()

        // Handle WiFi pairing
        if (pairAddress != null) {
            val code = pairingCode ?: run { echo("Error: --code is required with --pair"); return }
            echo("Pairing with $pairAddress...")
            val result = deployer.pair(pairAddress!!, code)
            echo(result.message)
            if (!result.success) throw com.github.ajalt.clikt.core.ProgramResult(3)
            return
        }

        // Handle WiFi connect
        if (connectAddress != null) {
            echo("Connecting to $connectAddress...")
            val result = deployer.connect(connectAddress!!)
            echo(result.message)
            if (!result.success) throw com.github.ajalt.clikt.core.ProgramResult(3)
            return
        }

        // Build then deploy
        echo("Building $variant variant at: $projectPath")
        val buildResult = GradleRunner().build(BuildRequest(projectDir = projectPath, variant = variant))
        if (!buildResult.success) {
            val error = buildResult.diagnostics.firstOrNull()
            echo("Build failed (exit ${buildResult.exitCode}): ${error?.message ?: "unknown error"}")
            throw com.github.ajalt.clikt.core.ProgramResult(buildResult.exitCode)
        }

        val apk = buildResult.artifacts.firstOrNull()
        if (apk == null) {
            echo("Build succeeded but no APK found")
            throw com.github.ajalt.clikt.core.ProgramResult(1)
        }

        echo("Installing ${apk.path}...")
        val deployResult = deployer.deploy(apk.path, deviceSerial)
        echo(deployResult.message)
        if (!deployResult.success) throw com.github.ajalt.clikt.core.ProgramResult(1)
    }
}
