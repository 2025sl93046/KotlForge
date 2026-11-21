package dev.kotlforge.watch

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class FileWatcherTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `detects file modification`() {
        val srcDir = tempDir.resolve("src").createDirectories()
        val file = srcDir.resolve("Main.kt").also { it.writeText("class Main") }

        val latch = CountDownLatch(1)
        val detected = mutableListOf<Path>()

        val watcher = FileWatcher(listOf(srcDir))
        watcher.start { paths ->
            detected.addAll(paths)
            latch.countDown()
        }

        Thread.sleep(200)
        file.writeText("class Main // updated")

        val fired = latch.await(3, TimeUnit.SECONDS)
        watcher.stop()

        assertTrue(fired, "Expected change event within 3 seconds")
        assertTrue(detected.any { it.fileName.toString() == "Main.kt" })
    }

    @Test
    fun `detects file creation`() {
        val srcDir = tempDir.resolve("src").createDirectories()
        val latch = CountDownLatch(1)
        val detected = mutableListOf<Path>()

        val watcher = FileWatcher(listOf(srcDir))
        watcher.start { paths ->
            detected.addAll(paths)
            latch.countDown()
        }

        Thread.sleep(200)
        srcDir.resolve("NewFile.kt").writeText("class NewFile")

        val fired = latch.await(3, TimeUnit.SECONDS)
        watcher.stop()

        assertTrue(fired, "Expected creation event within 3 seconds")
        assertTrue(detected.any { it.fileName.toString() == "NewFile.kt" })
    }

    @Test
    fun `ignores paths matching the predicate`() {
        val srcDir = tempDir.resolve("src").createDirectories()
        val buildDir = tempDir.resolve("build").createDirectories()
        buildDir.resolve("ignored.class").writeText("ignored")

        val latch = CountDownLatch(1)
        val detected = mutableListOf<Path>()

        val watcher = FileWatcher(listOf(srcDir, buildDir)) { path ->
            path.toString().contains("build")
        }
        watcher.start { paths ->
            detected.addAll(paths)
            latch.countDown()
        }

        Thread.sleep(200)
        // Write in the ignored dir — should not fire
        buildDir.resolve("another.class").writeText("also ignored")
        // Write in watched dir — should fire
        srcDir.resolve("Real.kt").writeText("class Real")

        val fired = latch.await(3, TimeUnit.SECONDS)
        watcher.stop()

        assertTrue(fired)
        assertTrue(detected.none { it.toString().contains("build") }, "Build files should be ignored")
        assertTrue(detected.any { it.fileName.toString() == "Real.kt" })
    }

    @Test
    fun `stop is idempotent`() {
        val srcDir = tempDir.resolve("src").createDirectories()
        val watcher = FileWatcher(listOf(srcDir))
        watcher.start { }
        Thread.sleep(100)
        watcher.stop()
        // Second stop must not throw
        assertDoesNotThrow { watcher.stop() }
    }

    @Test
    fun `watches newly created subdirectories`() {
        val srcDir = tempDir.resolve("src").createDirectories()
        val latch = CountDownLatch(1)
        val detected = mutableListOf<Path>()

        val watcher = FileWatcher(listOf(srcDir))
        watcher.start { paths ->
            detected.addAll(paths)
            if (detected.any { it.fileName.toString() == "Deep.kt" }) latch.countDown()
        }

        Thread.sleep(200)
        val subDir = srcDir.resolve("model").createDirectories()
        Thread.sleep(3000) // macOS WatchService polls with up to ~2s delay before delivering ENTRY_CREATE
        subDir.resolve("Deep.kt").writeText("class Deep")

        val fired = latch.await(10, TimeUnit.SECONDS)
        watcher.stop()

        assertTrue(fired, "Expected event from newly created subdirectory")
    }
}
