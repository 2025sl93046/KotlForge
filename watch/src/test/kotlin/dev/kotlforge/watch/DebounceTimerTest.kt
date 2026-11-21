package dev.kotlforge.watch

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DebounceTimerTest {

    @Test
    fun `fires once after a quiet period`() {
        val fired = CopyOnWriteArrayList<List<Int>>()
        val latch = CountDownLatch(1)
        val timer = DebounceTimer<Int>(100) { batch ->
            fired += batch
            latch.countDown()
        }

        timer.trigger(listOf(1))

        val completed = latch.await(2, TimeUnit.SECONDS)
        timer.shutdown()

        assertTrue(completed, "Expected timer to fire within 2 seconds")
        assertEquals(1, fired.size)
        assertEquals(listOf(1), fired.first())
    }

    @Test
    fun `coalesces rapid triggers into one batch`() {
        val fired = CopyOnWriteArrayList<List<Int>>()
        val latch = CountDownLatch(1)
        val timer = DebounceTimer<Int>(200) { batch ->
            fired += batch
            latch.countDown()
        }

        // Trigger 5 times rapidly — all should be batched together
        repeat(5) { i -> timer.trigger(listOf(i)) }

        val completed = latch.await(3, TimeUnit.SECONDS)
        timer.shutdown()

        assertTrue(completed)
        assertEquals(1, fired.size, "All rapid triggers should produce exactly one callback")
        assertEquals(5, fired.first().size, "Batch should contain all 5 items")
    }

    @Test
    fun `resets the window on each trigger`() {
        val fired = CopyOnWriteArrayList<List<Int>>()
        val timer = DebounceTimer<Int>(200) { batch -> fired += batch }

        timer.trigger(listOf(1))
        Thread.sleep(100)
        timer.trigger(listOf(2))
        Thread.sleep(100)
        timer.trigger(listOf(3))

        // After final trigger, wait for the window to pass
        Thread.sleep(400)
        timer.shutdown()

        assertEquals(1, fired.size, "Window should reset on each trigger")
        assertEquals(listOf(1, 2, 3), fired.first())
    }

    @Test
    fun `fires separate batches for separate quiet periods`() {
        val fired = CopyOnWriteArrayList<List<Int>>()
        val latch = CountDownLatch(2)
        val timer = DebounceTimer<Int>(100) { batch ->
            fired += batch
            latch.countDown()
        }

        timer.trigger(listOf(1))
        Thread.sleep(300) // let first batch fire
        timer.trigger(listOf(2))

        val completed = latch.await(3, TimeUnit.SECONDS)
        timer.shutdown()

        assertTrue(completed)
        assertEquals(2, fired.size)
        assertEquals(listOf(1), fired[0])
        assertEquals(listOf(2), fired[1])
    }
}
