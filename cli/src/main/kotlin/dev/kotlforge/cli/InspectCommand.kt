package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.kotlforge.core.ConfigLoader
import dev.kotlforge.core.ProjectRootFinder
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines

class InspectCommand : CliktCommand(
    name = "inspect",
    help = "Show project information without building"
) {
    private val projectPath by option("--project", "-p", help = "Path to the Android project").default(".")

    override fun run() {
        val startDir = Path.of(projectPath).toAbsolutePath().normalize()
        val root = ProjectRootFinder.find(startDir)
        if (root == null) {
            echo("No Android project found at: $startDir", err = true)
            echo("Hint: Directory must contain settings.gradle.kts or settings.gradle", err = true)
            throw ProgramResult(ExitCode.BAD_ARGS)
        }

        val gradlew = root.resolve("gradlew")
        val config  = ConfigLoader.load(root)
        val name    = readProjectName(root)
        val gradleVersion = readGradleVersion(root)

        echo("KotlForge Inspect: $root")
        echo("")
        echo("  Project root:    $root")
        echo("  Project name:    ${name ?: "(unknown)"}")
        echo("  Gradle wrapper:  ${if (gradlew.exists()) "found${if (gradleVersion != null) " (Gradle $gradleVersion)" else ""}" else "MISSING — run `gradle wrapper` to create it"}")
        echo("  Config file:     ${if (root.resolve("kotlforge.json").exists()) "kotlforge.json (found)" else "(none — using defaults)"}")
        echo("  Default variant: ${config.defaultVariant}")
        if (config.defaultTasks.isNotEmpty()) {
            echo("  Default tasks:   ${config.defaultTasks.joinToString(", ")}")
        }
        echo("")
        echo("  Status: OK")
    }

    private fun readProjectName(root: Path): String? {
        for (file in listOf("settings.gradle.kts", "settings.gradle")) {
            val path = root.resolve(file)
            if (!path.isRegularFile()) continue
            path.readLines().forEach { line ->
                val trimmed = line.trim()
                // rootProject.name = "foo" or rootProject.name("foo")
                val eqMatch = Regex("""rootProject\.name\s*=\s*["']([^"']+)["']""").find(trimmed)
                if (eqMatch != null) return eqMatch.groupValues[1]
                val fnMatch = Regex("""rootProject\.name\(["']([^"']+)["']\)""").find(trimmed)
                if (fnMatch != null) return fnMatch.groupValues[1]
            }
        }
        return null
    }

    private fun readGradleVersion(root: Path): String? {
        val props = root.resolve("gradle/wrapper/gradle-wrapper.properties")
        if (!props.isRegularFile()) return null
        props.readLines().forEach { line ->
            val match = Regex("""distributionUrl=.+/gradle-([^-]+)-""").find(line)
            if (match != null) return match.groupValues[1]
        }
        return null
    }
}
