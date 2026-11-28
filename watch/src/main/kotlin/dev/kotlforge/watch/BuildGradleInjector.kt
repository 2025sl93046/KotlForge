package dev.kotlforge.watch

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/**
 * Temporarily injects a JVMTI SwapAgent into the target project's app/build.gradle.kts
 * so the built APK can receive hot-swap bytecode via a localhost socket.
 *
 * The original file is backed up to ~/.kotlforge/backup/<project-hash>/
 * before any modification and is always restored on exit — even on crash.
 *
 * On next watch start, any leftover backup from a previous crash is restored first.
 */
class BuildGradleInjector(private val projectDir: File) {

    private val appBuildGradle = File(projectDir, "app/build.gradle.kts")
    private val backupDir = File(System.getProperty("user.home"), ".kotlforge/backup/${projectHash()}")
    private val backupFile = File(backupDir, "app-build.gradle.kts.bak")

    companion object {
        // Marker so we never double-inject
        const val INJECTION_MARKER = "// kotlforge-lightning-injected"

        // Minimal SwapAgent dependency block injected into the app module
        private val INJECTION_BLOCK = """

$INJECTION_MARKER
dependencies {
    debugImplementation("dev.kotlforge:swap-agent:0.1.0")
}
android {
    defaultConfig {
        // SwapAgent listens on this port for bytecode push from kotlforge watch
        buildConfigField("int", "KOTLFORGE_SWAP_PORT", "7777")
    }
}
"""
    }

    /**
     * Restores any leftover backup from a previous crash, then injects the SwapAgent block.
     * Returns true if injection was performed, false if already injected or build.gradle.kts not found.
     */
    fun inject(): Boolean {
        restoreIfBackupExists()

        if (!appBuildGradle.exists()) {
            println("[lightning] app/build.gradle.kts not found — lightning mode requires a standard app module layout")
            return false
        }

        val original = appBuildGradle.readText()
        if (original.contains(INJECTION_MARKER)) {
            println("[lightning] SwapAgent already injected")
            return true
        }

        backupDir.mkdirs()
        backupFile.writeText(original)
        appBuildGradle.appendText(INJECTION_BLOCK)
        println("[lightning] SwapAgent injected into app/build.gradle.kts (backup at ${backupFile.absolutePath})")
        return true
    }

    /**
     * Restores app/build.gradle.kts from the backup and removes the backup file.
     * Safe to call multiple times.
     */
    fun restore() {
        if (!backupFile.exists()) return
        appBuildGradle.writeText(backupFile.readText())
        backupFile.delete()
        backupDir.delete() // only deletes if empty
        println("[lightning] app/build.gradle.kts restored from backup")
    }

    fun restoreIfBackupExists() {
        if (backupFile.exists()) {
            println("[lightning] Found leftover backup from previous session — restoring app/build.gradle.kts")
            restore()
        }
    }

    private fun projectHash(): String {
        val bytes = MessageDigest.getInstance("MD5").digest(projectDir.absolutePath.toByteArray())
        return bytes.take(6).joinToString("") { "%02x".format(it) }
    }
}
