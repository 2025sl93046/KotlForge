package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.kotlforge.core.ConfigLoader
import dev.kotlforge.deploy.DeviceDeployer
import dev.kotlforge.watch.WatchEngine
import dev.kotlforge.watch.globToRegex
import java.io.File

class WatchCommand : CliktCommand(
    name = "watch",
    help = "Watch source files and rebuild automatically on change"
) {
    private val projectPath by option("--project", "-p", help = "Path to the Android project").default(".")
    private val variant by option("--variant", "-v", help = "Build variant (debug/release)").default("debug")
    private val deviceSerial by option(
        "--deploy", "-d",
        help = "Deploy APK to this device serial after each successful build (auto-select if flag is given without a value)"
    )
    private val autoDeploy by option("--auto-deploy", help = "Deploy to auto-selected device after each successful build").flag()

    override fun run() {
        val projectDir = File(projectPath).canonicalFile
        val config = ConfigLoader.load(projectDir.toPath())

        val watchPaths = config.watchPaths
            .map { projectDir.toPath().resolve(it) }
            .filter { it.toFile().exists() }
            .ifEmpty { listOf(projectDir.toPath()) }

        val ignorePatterns = config.watchIgnorePatterns.map { globToRegex(it) }

        val deployer = if (deviceSerial != null || autoDeploy) DeviceDeployer() else null

        WatchEngine(deployer = deployer).watch(
            projectDir = projectDir,
            variant = variant,
            watchPaths = watchPaths,
            ignorePatterns = ignorePatterns,
            deviceSerial = deviceSerial
        )
    }
}
