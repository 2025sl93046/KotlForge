package dev.kotlforge.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile

class ProjectRootFinderTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `finds root when settings gradle kts is in the given dir`() {
        tempDir.resolve("settings.gradle.kts").createFile()

        val result = ProjectRootFinder.find(tempDir)

        assertEquals(tempDir, result)
    }

    @Test
    fun `finds root when settings gradle is in the given dir`() {
        tempDir.resolve("settings.gradle").createFile()

        val result = ProjectRootFinder.find(tempDir)

        assertEquals(tempDir, result)
    }

    @Test
    fun `finds root in parent directory when starting from a subdirectory`() {
        tempDir.resolve("settings.gradle.kts").createFile()
        val subDir = tempDir.resolve("app/src/main").createDirectories()

        val result = ProjectRootFinder.find(subDir)

        assertEquals(tempDir, result)
    }

    @Test
    fun `returns null when no settings file exists anywhere`() {
        val subDir = tempDir.resolve("some/nested/dir").createDirectories()

        val result = ProjectRootFinder.find(subDir)

        assertNull(result)
    }

    @Test
    fun `prefers settings gradle kts over settings gradle when both present`() {
        tempDir.resolve("settings.gradle.kts").createFile()
        tempDir.resolve("settings.gradle").createFile()

        val result = ProjectRootFinder.find(tempDir)

        assertEquals(tempDir, result)
    }

    @Test
    fun `finds nearest root when multiple settings files exist in hierarchy`() {
        tempDir.resolve("settings.gradle.kts").createFile()
        val innerProject = tempDir.resolve("inner").createDirectories()
        innerProject.resolve("settings.gradle.kts").createFile()

        val result = ProjectRootFinder.find(innerProject)

        assertEquals(innerProject, result)
    }
}
