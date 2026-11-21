package dev.kotlforge.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class ConfigLoaderTest {

    @TempDir
    lateinit var projectRoot: Path

    @Test
    fun `returns default config when no file present`() {
        val config = ConfigLoader.load(projectRoot)

        assertEquals("debug", config.defaultVariant)
        assertTrue(config.defaultTasks.isEmpty())
        assertNull(config.jdkPath)
    }

    @Test
    fun `loads valid kotlforge json`() {
        projectRoot.resolve("kotlforge.json").writeText(
            """{"defaultVariant":"release","defaultTasks":["assembleRelease","lint"]}"""
        )

        val config = ConfigLoader.load(projectRoot)

        assertEquals("release", config.defaultVariant)
        assertEquals(listOf("assembleRelease", "lint"), config.defaultTasks)
    }

    @Test
    fun `ignores unknown fields in config file`() {
        projectRoot.resolve("kotlforge.json").writeText(
            """{"defaultVariant":"staging","unknownField":"ignored"}"""
        )

        val config = ConfigLoader.load(projectRoot)

        assertEquals("staging", config.defaultVariant)
    }

    @Test
    fun `returns default config when file is malformed json`() {
        projectRoot.resolve("kotlforge.json").writeText("{ not valid json }")

        val config = ConfigLoader.load(projectRoot)

        assertEquals("debug", config.defaultVariant)
        assertTrue(config.defaultTasks.isEmpty())
    }

    @Test
    fun `loads jdkPath when present`() {
        projectRoot.resolve("kotlforge.json").writeText(
            """{"jdkPath":"/Library/Java/sapmachine-17"}"""
        )

        val config = ConfigLoader.load(projectRoot)

        assertEquals("/Library/Java/sapmachine-17", config.jdkPath)
    }
}
