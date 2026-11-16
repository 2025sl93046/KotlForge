package dev.kotlforge.diagnostics

data class ToolchainInfo(
    val jdkPath: String?,
    val androidSdkPath: String?,
    val gradleVersion: String?
)

class ToolchainDetector {

    fun detect(): ToolchainInfo {
        return ToolchainInfo(
            jdkPath = System.getenv("JAVA_HOME"),
            androidSdkPath = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT"),
            gradleVersion = null
        )
    }

    fun validate(info: ToolchainInfo): List<String> {
        val errors = mutableListOf<String>()
        if (info.jdkPath == null) errors.add("JAVA_HOME is not set")
        if (info.androidSdkPath == null) errors.add("ANDROID_HOME is not set")
        return errors
    }
}
