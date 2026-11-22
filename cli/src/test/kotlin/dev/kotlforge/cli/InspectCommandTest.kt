package dev.kotlforge.cli

import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class InspectCommandTest {

    @Test
    fun `valid project path exits with 0`(@TempDir tempDir: Path) {
        tempDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "test-app"""")

        val result = InspectCommand().test("--project", tempDir.toString())

        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains("Project root:"))
        assertTrue(result.output.contains("Status: OK"))
    }

    @Test
    fun `invalid project path exits with 2`(@TempDir tempDir: Path) {
        val nonexistent = tempDir.resolve("does-not-exist").toString()

        val result = InspectCommand().test("--project", nonexistent)

        assertEquals(ExitCode.BAD_ARGS, result.statusCode)
    }

    @Test
    fun `json format outputs valid JSON`(@TempDir tempDir: Path) {
        tempDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "my-app"""")

        val result = InspectCommand().test("--project", tempDir.toString(), "--format", "json")

        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains(""""projectRoot""""))
        assertTrue(result.output.contains(""""projectName""""))
        assertTrue(result.output.contains(""""defaultVariant""""))
    }

    @Test
    fun `project name is read from settings file`(@TempDir tempDir: Path) {
        tempDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "my-awesome-app"""")

        val result = InspectCommand().test("--project", tempDir.toString())

        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains("my-awesome-app"))
    }
}
