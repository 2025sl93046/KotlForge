package dev.kotlforge.cli

import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DoctorCommandTest {

    @Test
    fun `help flag exits with 0 and shows usage`() {
        val result = DoctorCommand().test("--help")

        assertEquals(0, result.statusCode)
        assertTrue(result.output.contains("doctor"))
    }

    @Test
    fun `unknown option exits non-zero`() {
        val result = DoctorCommand().test("--unknown-flag")

        assertNotEquals(0, result.statusCode)
    }
}
