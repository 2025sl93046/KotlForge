package dev.kotlforge.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class KotlForgeTest {

    @TempDir
    lateinit var projectDir: Path

    private val engine = mockk<BuildEngine>()

    @Test
    fun `delegates valid request to engine`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        val request = BuildRequest(projectDir = projectDir.toString())
        val expected = BuildResult(success = true, exitCode = 0, durationMillis = 1000)
        every { engine.build(any()) } returns expected

        val result = KotlForge(engine).build(request)

        assertEquals(expected, result)
        verify(exactly = 1) { engine.build(any()) }
    }

    @Test
    fun `returns error result for blank projectDir`() {
        val request = BuildRequest(projectDir = "  ")

        val result = KotlForge(engine).build(request)

        assertFalse(result.success)
        assertEquals(2, result.exitCode)
        assertTrue(result.diagnostics.any { it.message.contains("projectDir") })
        verify(exactly = 0) { engine.build(any()) }
    }

    @Test
    fun `returns error result for blank variant`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        val request = BuildRequest(projectDir = projectDir.toString(), variant = "")

        val result = KotlForge(engine).build(request)

        assertFalse(result.success)
        assertEquals(2, result.exitCode)
        assertTrue(result.diagnostics.any { it.message.contains("variant") })
        verify(exactly = 0) { engine.build(any()) }
    }

    @Test
    fun `all validation errors have ERROR level and suggestion`() {
        val request = BuildRequest(projectDir = "", variant = "")

        val result = KotlForge(engine).build(request)

        assertTrue(result.diagnostics.isNotEmpty())
        result.diagnostics.forEach {
            assertEquals(DiagnosticLevel.ERROR, it.level)
            assertNotNull(it.suggestion)
        }
    }

    @Test
    fun `applies defaultVariant from config when variant is default debug`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        projectDir.resolve("kotlforge.json").writeText("""{"defaultVariant":"release"}""")
        val request = BuildRequest(projectDir = projectDir.toString(), variant = "debug")
        every { engine.build(any()) } returns BuildResult(success = true, exitCode = 0, durationMillis = 0)

        KotlForge(engine).build(request)

        verify { engine.build(match { it.variant == "release" }) }
    }

    @Test
    fun `does not override variant when explicitly set to non-debug value`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        projectDir.resolve("kotlforge.json").writeText("""{"defaultVariant":"release"}""")
        val request = BuildRequest(projectDir = projectDir.toString(), variant = "staging")
        every { engine.build(any()) } returns BuildResult(success = true, exitCode = 0, durationMillis = 0)

        KotlForge(engine).build(request)

        verify { engine.build(match { it.variant == "staging" }) }
    }

    @Test
    fun `applies defaultTasks from config when request tasks are empty`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        projectDir.resolve("kotlforge.json").writeText("""{"defaultTasks":["assembleRelease","lint"]}""")
        val request = BuildRequest(projectDir = projectDir.toString())
        every { engine.build(any()) } returns BuildResult(success = true, exitCode = 0, durationMillis = 0)

        KotlForge(engine).build(request)

        verify { engine.build(match { it.tasks == listOf("assembleRelease", "lint") }) }
    }

    @Test
    fun `passes through engine result on success`() {
        projectDir.resolve("settings.gradle.kts").writeText("")
        val artifact = BuildArtifact(ArtifactType.APK, "app/build/outputs/apk/release/app-release.apk", 3000000)
        val expected = BuildResult(success = true, exitCode = 0, durationMillis = 5000, artifacts = listOf(artifact))
        val request = BuildRequest(projectDir = projectDir.toString(), variant = "release")
        every { engine.build(any()) } returns expected

        val result = KotlForge(engine).build(request)

        assertTrue(result.success)
        assertEquals(1, result.artifacts.size)
        assertEquals(ArtifactType.APK, result.artifacts[0].type)
    }
}
