package dev.kotlforge.watch

import dev.kotlforge.core.BuildEngine
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.deploy.DeviceDeployer
import dev.kotlforge.deploy.InstallMode
import dev.kotlforge.engine.GradleRunner
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class WatchEngine(
    private val buildEngine: BuildEngine = GradleRunner(),
    private val deployer: DeviceDeployer? = null,
    private val deployMode: DeployMode = DeployMode.STANDARD
) {

    fun watch(
        projectDir: File,
        variant: String,
        watchPaths: List<Path>,
        ignorePatterns: List<Regex> = emptyList(),
        deviceSerial: String? = null,
        debounceMillis: Long = 300,
        onCycle: (WatchCycleResult) -> Unit = {}
    ) {
        val latch = CountDownLatch(1)
        val cycleCount = AtomicInteger(0)

        val injector = if (deployMode == DeployMode.LIGHTNING) {
            BuildGradleInjector(projectDir).also { inj ->
                if (!inj.inject()) {
                    println("[lightning] Could not inject SwapAgent — falling back to standard deploy")
                    return watch(projectDir, variant, watchPaths, ignorePatterns, deviceSerial, debounceMillis, onCycle)
                }
                println("[lightning] Lightning mode active — app state will be preserved across changes")
                println("[lightning] NOTE: structural changes (new fields/classes) will trigger a full reinstall automatically")
            }
        } else null

        val debouncer = DebounceTimer<Path>(debounceMillis) { changedFiles ->
            val result = runCycle(
                projectDir = projectDir,
                variant = variant,
                changedFiles = changedFiles,
                cycleNum = cycleCount.incrementAndGet(),
                deviceSerial = deviceSerial
            )
            onCycle(result)
        }

        val watcher = FileWatcher(watchPaths) { path ->
            val normalized = path.toString().replace('\\', '/')
            ignorePatterns.any { it.containsMatchIn(normalized) }
        }

        val shutdownHook = Thread({
            println("\n[watch] Shutting down...")
            watcher.stop()
            debouncer.shutdown()
            injector?.restore()
            stopGradleDaemon(projectDir)
            latch.countDown()
        }, "kotlforge-shutdown")
        Runtime.getRuntime().addShutdownHook(shutdownHook)

        println("[watch] Watching ${watchPaths.size} path(s). Press Ctrl+C to stop.")
        watcher.start { changedFiles -> debouncer.trigger(changedFiles) }

        try {
            latch.await()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            watcher.stop()
            debouncer.shutdown()
            injector?.restore()
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook)
            } catch (_: IllegalStateException) {}
        }
    }

    internal fun runCycle(
        projectDir: File,
        variant: String,
        changedFiles: List<Path>,
        cycleNum: Int,
        deviceSerial: String?
    ): WatchCycleResult {
        val start = System.currentTimeMillis()

        println("\n[watch] Cycle $cycleNum — ${changedFiles.size} file(s) changed:")
        changedFiles.take(5).forEach { println("  ${it.fileName}") }
        if (changedFiles.size > 5) println("  ... and ${changedFiles.size - 5} more")
        println("[watch] Building $variant...")

        val buildResult = buildEngine.build(BuildRequest(projectDir = projectDir.absolutePath, variant = variant))
        val duration = System.currentTimeMillis() - start

        if (!buildResult.success) {
            println("[watch] Build failed in ${duration}ms (exit ${buildResult.exitCode})")
            buildResult.diagnostics.firstOrNull()?.let { println("[watch]   ${it.message}") }
            return WatchCycleResult(cycleNum, changedFiles, buildResult, null, duration)
        }

        println("[watch] Build succeeded in ${duration}ms")
        buildResult.artifacts.forEach { println("[watch]   APK: ${it.path}") }

        if (deployer == null) {
            return WatchCycleResult(cycleNum, changedFiles, buildResult, null, duration)
        }

        val apk = buildResult.artifacts.firstOrNull() ?: run {
            println("[watch] No APK to deploy")
            return WatchCycleResult(cycleNum, changedFiles, buildResult, null, duration)
        }

        return when (deployMode) {
            DeployMode.LIGHTNING -> runLightningDeploy(apk.path, projectDir, variant, cycleNum, changedFiles, buildResult, deviceSerial, duration)
            DeployMode.INCREMENTAL -> runInstallDeploy(apk.path, deviceSerial, cycleNum, changedFiles, buildResult, duration, InstallMode.INCREMENTAL)
            DeployMode.STANDARD -> runInstallDeploy(apk.path, deviceSerial, cycleNum, changedFiles, buildResult, duration, InstallMode.STANDARD)
        }
    }

    private fun runInstallDeploy(
        apkPath: String,
        deviceSerial: String?,
        cycleNum: Int,
        changedFiles: List<Path>,
        buildResult: dev.kotlforge.core.BuildResult,
        duration: Long,
        installMode: InstallMode
    ): WatchCycleResult {
        val modeLabel = if (installMode == InstallMode.INCREMENTAL) "[incremental] " else ""
        println("[watch] ${modeLabel}Deploying $apkPath...")
        val deployResult = deployer!!.deploy(apkPath, deviceSerial, installMode)
        return if (deployResult.success) {
            println("[watch] Deployed to ${deployResult.deviceSerial}")
            WatchCycleResult(cycleNum, changedFiles, buildResult, deployResult.deviceSerial, duration)
        } else {
            println("[watch] Deploy failed: ${deployResult.message}")
            WatchCycleResult(cycleNum, changedFiles, buildResult, null, duration)
        }
    }

    private fun runLightningDeploy(
        apkPath: String,
        projectDir: File,
        variant: String,
        cycleNum: Int,
        changedFiles: List<Path>,
        buildResult: dev.kotlforge.core.BuildResult,
        deviceSerial: String?,
        duration: Long
    ): WatchCycleResult {
        val swapClient = SwapAgentClient()

        if (!swapClient.isAgentReachable()) {
            // Agent not running yet — full install to get agent onto device
            println("[lightning] SwapAgent not running on device — doing full install to start it")
            return runInstallDeploy(apkPath, deviceSerial, cycleNum, changedFiles, buildResult, duration, InstallMode.STANDARD)
        }

        val dexDir = File(projectDir, "app/build/intermediates/dex/$variant")
        val dexFiles = dexDir.walkTopDown().filter { it.extension == "dex" }.toList()

        println("[lightning] Pushing ${dexFiles.size} dex file(s) to SwapAgent...")
        return when (val swapResult = swapClient.pushDex(dexFiles)) {
            is SwapResult.Success -> {
                println("[lightning] Hot-swap complete — app state preserved")
                WatchCycleResult(cycleNum, changedFiles, buildResult, "lightning-swap", duration)
            }
            is SwapResult.StructuralChange -> {
                // New field/class added — JVMTI cannot handle this, fall back to full reinstall
                println("[lightning] Structural change detected (new field or class) — falling back to full reinstall")
                runInstallDeploy(apkPath, deviceSerial, cycleNum, changedFiles, buildResult, duration, InstallMode.STANDARD)
            }
            is SwapResult.AgentNotReachable -> {
                println("[lightning] SwapAgent not reachable — falling back to full reinstall")
                runInstallDeploy(apkPath, deviceSerial, cycleNum, changedFiles, buildResult, duration, InstallMode.STANDARD)
            }
            is SwapResult.NoDexFiles -> {
                println("[lightning] No dex files found — falling back to full reinstall")
                runInstallDeploy(apkPath, deviceSerial, cycleNum, changedFiles, buildResult, duration, InstallMode.STANDARD)
            }
            is SwapResult.AgentError -> {
                println("[lightning] SwapAgent error: ${swapResult.reason} — falling back to full reinstall")
                runInstallDeploy(apkPath, deviceSerial, cycleNum, changedFiles, buildResult, duration, InstallMode.STANDARD)
            }
        }
    }

    private fun stopGradleDaemon(projectDir: File) {
        val name = if (System.getProperty("os.name").lowercase().contains("win")) "gradlew.bat" else "gradlew"
        val wrapper = File(projectDir, name)
        if (!wrapper.exists()) return
        try {
            ProcessBuilder(wrapper.absolutePath, "--stop")
                .directory(projectDir)
                .start()
                .waitFor(10, TimeUnit.SECONDS)
        } catch (_: Exception) {}
    }
}
