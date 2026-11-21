package dev.kotlforge.deploy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class DeviceStoreTest {

    @Test
    fun `saves and loads a paired device`(@TempDir tempDir: Path) {
        val store = DeviceStore(File(tempDir.toFile(), "devices.json"))

        store.save(PairedDevice(ipPort = "192.168.1.5:5555"))

        val devices = store.loadAll()
        assertEquals(1, devices.size)
        assertEquals("192.168.1.5:5555", devices[0].ipPort)
    }

    @Test
    fun `does not add duplicate entries`(@TempDir tempDir: Path) {
        val store = DeviceStore(File(tempDir.toFile(), "devices.json"))

        store.save(PairedDevice(ipPort = "192.168.1.5:5555"))
        store.save(PairedDevice(ipPort = "192.168.1.5:5555"))

        assertEquals(1, store.loadAll().size)
    }

    @Test
    fun `saves multiple different devices`(@TempDir tempDir: Path) {
        val store = DeviceStore(File(tempDir.toFile(), "devices.json"))

        store.save(PairedDevice(ipPort = "192.168.1.5:5555"))
        store.save(PairedDevice(ipPort = "192.168.1.6:5555"))

        assertEquals(2, store.loadAll().size)
    }

    @Test
    fun `removes a device`(@TempDir tempDir: Path) {
        val store = DeviceStore(File(tempDir.toFile(), "devices.json"))

        store.save(PairedDevice(ipPort = "192.168.1.5:5555"))
        store.remove("192.168.1.5:5555")

        assertTrue(store.loadAll().isEmpty())
    }

    @Test
    fun `returns empty list when file does not exist`(@TempDir tempDir: Path) {
        val store = DeviceStore(File(tempDir.toFile(), "devices.json"))

        assertTrue(store.loadAll().isEmpty())
    }

    @Test
    fun `returns empty list when file is malformed`(@TempDir tempDir: Path) {
        val file = File(tempDir.toFile(), "devices.json")
        file.writeText("not valid json")
        val store = DeviceStore(file)

        assertTrue(store.loadAll().isEmpty())
    }
}
