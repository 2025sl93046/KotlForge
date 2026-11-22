package dev.kotlforge.cli

import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BuildCommandTest {

    @Test
    fun `invalid format value exits with 2`() {
        val result = BuildCommand().test("--format", "xml")

        assertEquals(ExitCode.BAD_ARGS, result.statusCode)
    }

    @Test
    fun `unknown option exits with 2`() {
        val result = BuildCommand().test("--unknown-flag")

        assertEquals(ExitCode.BAD_ARGS, result.statusCode)
    }

    @Test
    fun `help flag exits with 0`() {
        val result = BuildCommand().test("--help")

        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains("--project"))
        assertTrue(result.output.contains("--variant"))
        assertTrue(result.output.contains("--format"))
    }
}
