package dev.kotlforge.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import dev.kotlforge.core.ConfigLoader
import dev.kotlforge.core.ProjectRootFinder
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class InspectCommand : CliktCommand(
    name = "inspect",
    help = "Show project information without building"
) {
    private val projectPath by option("--project", "-p", help = "Path to the Android project").default(".")
    private val format by option("--format", "-f", help = "Output format: text or json")
        .choice("text", "json").default("text")

    private val json = Json { prettyPrint = true }

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
        val configPresent = root.resolve("kotlforge.json").exists()

        if (format == "json") {
            val report = ProjectInfo(
                projectRoot = root.toString(),
                projectName = name,
                gradleWrapperPresent = gradlew.exists(),
                gradleVersion = gradleVersion,
                configFilePresent = configPresent,
                defaultVariant = config.defaultVariant,
                defaultTasks = config.defaultTasks
            )
            echo(json.encodeToString(ProjectInfo.serializer(), report))
            return
        }

        val wrapperStatus = if (gradlew.exists()) {
            "found${if (gradleVersion != null) " (Gradle $gradleVersion)" else ""}"
        } else {
            "MISSING — run `gradle wrapper` to create it"
        }
        val configStatus = if (configPresent) "kotlforge.json (found)" else "(none — using defaults)"

        echo("KotlForge Inspect: $root")
        echo("")
        echo("  Project root:    $root")
        echo("  Project name:    ${name ?: "(unknown)"}")
        echo("  Gradle wrapper:  $wrapperStatus")
        echo("  Config file:     $configStatus")
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

@Serializable
data class ProjectInfo(
    val projectRoot: String,
    val projectName: String?,
    val gradleWrapperPresent: Boolean,
    val gradleVersion: String?,
    val configFilePresent: Boolean,
    val defaultVariant: String,
    val defaultTasks: List<String>
)
