package dev.kotlforge.watch

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class FileWatcher(
    private val roots: List<Path>,
    private val shouldIgnore: (Path) -> Boolean = { false }
) {
    private val watchService: WatchService = FileSystems.getDefault().newWatchService()
    private val keys = mutableMapOf<WatchKey, Path>()
    private val running = AtomicBoolean(false)
    private var watchThread: Thread? = null

    fun start(onChange: (List<Path>) -> Unit) {
        roots.forEach { root ->
            if (Files.exists(root)) registerTree(root)
        }
        running.set(true)
        watchThread = Thread({
            while (running.get()) {
                val key = watchService.poll(100, TimeUnit.MILLISECONDS) ?: continue
                val dir = keys[key] ?: run { key.reset(); continue }
                val changed = mutableListOf<Path>()
                for (event in key.pollEvents()) {
                    if (event.kind() == OVERFLOW) continue
                    @Suppress("UNCHECKED_CAST")
                    val ctx = (event as? WatchEvent<Path>)?.context() ?: continue
                    val path = dir.resolve(ctx)
                    if (shouldIgnore(path)) continue
                    changed.add(path)
                    if (event.kind() == ENTRY_CREATE && Files.isDirectory(path)) {
                        registerTree(path)
                    }
                }
                if (changed.isNotEmpty()) onChange(changed)
                if (!key.reset()) keys.remove(key)
            }
        }, "kotlforge-watcher").also { it.isDaemon = true }
        watchThread!!.start()
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        watchThread?.join(1000)
        try {
            watchService.close()
        } catch (_: Exception) {}
    }

    private fun registerTree(root: Path) {
        try {
            Files.walk(root)
                .filter { Files.isDirectory(it) }
                .forEach { dir ->
                    if (!shouldIgnore(dir)) {
                        try {
                            val key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                            keys[key] = dir
                        } catch (_: Exception) {}
                    }
                }
        } catch (_: Exception) {}
    }
}
