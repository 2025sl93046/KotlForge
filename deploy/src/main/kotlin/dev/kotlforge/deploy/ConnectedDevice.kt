package dev.kotlforge.deploy

data class ConnectedDevice(
    val serial: String,
    val state: String
) {
    val isOnline: Boolean get() = state == "device"
}
