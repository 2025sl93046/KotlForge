package dev.kotlforge.watch

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class DebounceTimer<T>(
    private val delayMillis: Long,
    private val onFired: (List<T>) -> Unit
) {
    private val executor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "kotlforge-debounce").also { it.isDaemon = true }
    }
    private val pending = CopyOnWriteArrayList<T>()
    @Volatile private var scheduled: ScheduledFuture<*>? = null

    fun trigger(items: List<T>) {
        pending.addAll(items)
        scheduled?.cancel(false)
        scheduled = executor.schedule({
            val batch = pending.toList()
            pending.clear()
            if (batch.isNotEmpty()) onFired(batch)
        }, delayMillis, TimeUnit.MILLISECONDS)
    }

    fun shutdown() {
        executor.shutdown()
    }
}
