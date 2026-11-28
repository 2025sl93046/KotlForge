package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.kotlforge.core.ConfigLoader
import dev.kotlforge.deploy.DeviceDeployer
import dev.kotlforge.watch.DeployMode
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
        help = "Deploy APK to this device serial after each successful build"
    )
    private val autoDeploy by option("--auto-deploy", help = "Deploy to auto-selected device after each successful build").flag()
    private val incremental by option(
        "--incremental",
        help = "Use adb install --incremental to stream only changed APK blocks (Android 11+, falls back automatically on older devices)"
    ).flag()
    private val lightning by option(
        "--lightning",
        help = "Hot-swap bytecode via JVMTI — no app restart, state preserved. Temporarily modifies app/build.gradle.kts (restored on exit). Requires Android 8+."
    ).flag()

    override fun run() {
        val projectDir = File(projectPath).canonicalFile
        val config = ConfigLoader.load(projectDir.toPath())

        val watchPaths = config.watchPaths
            .map { projectDir.toPath().resolve(it) }
            .filter { it.toFile().exists() }
            .ifEmpty { listOf(projectDir.toPath()) }

        val ignorePatterns = config.watchIgnorePatterns.map { globToRegex(it) }

        val deployer = if (deviceSerial != null || autoDeploy) DeviceDeployer() else null

        val deployMode = when {
            lightning -> DeployMode.LIGHTNING
            incremental -> DeployMode.INCREMENTAL
            else -> DeployMode.STANDARD
        }

        if (lightning) {
            echo("[lightning] WARNING: lightning mode temporarily modifies app/build.gradle.kts in your project.")
            echo("[lightning] The original file is backed up and restored on exit (even on crash).")
            echo("[lightning] Only method body changes are hot-swappable — structural changes fall back to full reinstall.")
        }

        WatchEngine(deployer = deployer, deployMode = deployMode).watch(
            projectDir = projectDir,
            variant = variant,
            watchPaths = watchPaths,
            ignorePatterns = ignorePatterns,
            deviceSerial = deviceSerial
        )
    }
}
