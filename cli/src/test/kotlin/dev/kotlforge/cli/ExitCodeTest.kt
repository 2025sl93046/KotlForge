package dev.kotlforge.cli

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExitCodeTest {

    @Test
    fun `SUCCESS is 0`() = assertEquals(0, ExitCode.SUCCESS)

    @Test
    fun `BUILD_FAILED is 1`() = assertEquals(1, ExitCode.BUILD_FAILED)

    @Test
    fun `BAD_ARGS is 2`() = assertEquals(2, ExitCode.BAD_ARGS)

    @Test
    fun `ENV_FAILED is 3`() = assertEquals(3, ExitCode.ENV_FAILED)
}
