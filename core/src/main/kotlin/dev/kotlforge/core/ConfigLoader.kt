package dev.kotlforge.core

import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

object ConfigLoader {

    private const val CONFIG_FILE = "kotlforge.json"

    private val json = Json { ignoreUnknownKeys = true }

    fun load(projectRoot: Path): KotlForgeConfig {
        val configFile = projectRoot.resolve(CONFIG_FILE)
        if (!configFile.exists()) return KotlForgeConfig()
        return try {
            json.decodeFromString(KotlForgeConfig.serializer(), configFile.readText())
        } catch (_: Exception) {
            KotlForgeConfig()
        }
    }
}
