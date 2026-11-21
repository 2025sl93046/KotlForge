package dev.kotlforge.deploy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

@Serializable
data class PairedDevice(
    val ipPort: String,
    val nickname: String? = null
)

class DeviceStore(
    private val storeFile: File = File(System.getProperty("user.home"), ".kotlforge/devices.json")
) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun save(device: PairedDevice) {
        storeFile.parentFile.mkdirs()
        val existing = loadAll().toMutableList()
        if (existing.none { it.ipPort == device.ipPort }) {
            existing.add(device)
        }
        storeFile.writeText(json.encodeToString(existing))
    }

    fun loadAll(): List<PairedDevice> {
        if (!storeFile.exists()) return emptyList()
        return try {
            json.decodeFromString(storeFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun remove(ipPort: String) {
        val remaining = loadAll().filter { it.ipPort != ipPort }
        storeFile.parentFile.mkdirs()
        storeFile.writeText(json.encodeToString(remaining))
    }

    companion object {
        val default = DeviceStore()
    }
}
