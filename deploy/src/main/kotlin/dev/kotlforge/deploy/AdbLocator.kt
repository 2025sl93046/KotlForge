package dev.kotlforge.deploy

import java.io.File

object AdbLocator {

    fun locate(): File? {
        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val adb = File(androidHome, "platform-tools/${adbName()}")
            if (adb.exists()) return adb
        }
        return findOnPath()
    }

    private fun findOnPath(): File? {
        val path = System.getenv("PATH") ?: return null
        return path.split(File.pathSeparator)
            .map { File(it, adbName()) }
            .firstOrNull { it.exists() }
    }

    private fun adbName() = if (isWindows()) "adb.exe" else "adb"

    private fun isWindows() = System.getProperty("os.name").lowercase().contains("win")
}
