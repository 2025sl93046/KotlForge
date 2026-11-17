package dev.kotlforge.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.file.Path

class BuildRequestTest {

    @Test
    fun `default variant is debug`() {
        val req = BuildRequest(projectDir = "/tmp/my-app")
        assertEquals("debug", req.variant)
    }

    @Test
    fun `default tasks list is empty`() {
        val req = BuildRequest(projectDir = "/tmp/my-app")
        assertTrue(req.tasks.isEmpty())
    }

    @Test
    fun `resolvedProjectDir returns correct Path`() {
        val req = BuildRequest(projectDir = "/tmp/my-app")
        assertEquals(Path.of("/tmp/my-app"), req.resolvedProjectDir())
    }

    @Test
    fun `custom variant and tasks are preserved`() {
        val req = BuildRequest(
            projectDir = "/tmp/my-app",
            variant = "release",
            tasks = listOf("assembleRelease", "test")
        )
        assertEquals("release", req.variant)
        assertEquals(listOf("assembleRelease", "test"), req.tasks)
    }

    @Test
    fun `serializes to JSON and back`() {
        val req = BuildRequest(projectDir = "/tmp/my-app", variant = "release")
        val json = Json.encodeToString(req)
        val decoded = Json.decodeFromString<BuildRequest>(json)
        assertEquals(req, decoded)
    }
}
