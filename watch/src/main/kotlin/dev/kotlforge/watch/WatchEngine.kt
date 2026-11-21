package dev.kotlforge.watch

import dev.kotlforge.core.BuildEngine
import dev.kotlforge.core.BuildRequest
import dev.kotlforge.deploy.DeviceDeployer
import dev.kotlforge.engine.GradleRunner
import java.io.File
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class WatchEngine(
    private val buildEngine: BuildEngine = GradleRunner(),
    private val deployer: DeviceDeployer? = null
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

        println("[watch] Deploying ${apk.path}...")
        val deployResult = deployer.deploy(apk.path, deviceSerial)
        return if (deployResult.success) {
            println("[watch] Deployed to ${deployResult.deviceSerial}")
            WatchCycleResult(cycleNum, changedFiles, buildResult, deployResult.deviceSerial, duration)
        } else {
            println("[watch] Deploy failed: ${deployResult.message}")
            WatchCycleResult(cycleNum, changedFiles, buildResult, null, duration)
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
