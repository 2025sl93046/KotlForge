package dev.kotlforge.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ToolchainInfoTest {

    @Test
    fun `all fields are null by default`() {
        val info = ToolchainInfo()
        assertNull(info.jdkPath)
        assertNull(info.jdkVersion)
        assertNull(info.androidSdkPath)
        assertNull(info.gradleVersion)
        assertNull(info.agpVersion)
    }

    @Test
    fun `fully populated toolchain info is preserved`() {
        val info = ToolchainInfo(
            jdkPath = "/usr/lib/jvm/java-17",
            jdkVersion = "17.0.9",
            androidSdkPath = "/home/user/Android/Sdk",
            gradleVersion = "8.11",
            agpVersion = "8.2.0"
        )
        assertEquals("/usr/lib/jvm/java-17", info.jdkPath)
        assertEquals("17.0.9", info.jdkVersion)
        assertEquals("/home/user/Android/Sdk", info.androidSdkPath)
        assertEquals("8.11", info.gradleVersion)
        assertEquals("8.2.0", info.agpVersion)
    }

    @Test
    fun `partial toolchain info is valid`() {
        val info = ToolchainInfo(jdkPath = "/usr/lib/jvm/java-17", jdkVersion = "17.0.9")
        assertNotNull(info.jdkPath)
        assertNull(info.androidSdkPath)
        assertNull(info.gradleVersion)
    }

    @Test
    fun `serializes to JSON and back`() {
        val info = ToolchainInfo(
            jdkPath = "/usr/lib/jvm/java-17",
            jdkVersion = "17.0.9",
            androidSdkPath = "/home/user/Android/Sdk",
            gradleVersion = "8.11",
            agpVersion = "8.2.0"
        )
        val json = Json.encodeToString(info)
        val decoded = Json.decodeFromString<ToolchainInfo>(json)
        assertEquals(info, decoded)
    }

    @Test
    fun `null fields serialize correctly`() {
        val info = ToolchainInfo(jdkPath = "/usr/lib/jvm/java-17")
        val json = Json { explicitNulls = false }.encodeToString(info)
        val decoded = Json.decodeFromString<ToolchainInfo>(json)
        assertEquals(info.jdkPath, decoded.jdkPath)
        assertNull(decoded.androidSdkPath)
    }
}
