package dev.kotlforge.watch

import java.io.DataOutputStream
import java.io.File
import java.net.Socket
import java.net.ConnectException

/**
 * Pushes compiled .dex bytecode to the SwapAgent running inside the app process
 * via a localhost ADB-forwarded socket on [port].
 *
 * Protocol (simple length-prefixed binary):
 *   4 bytes  — number of class entries (big-endian int)
 *   per entry:
 *     2 bytes  — class name length
 *     N bytes  — class name (UTF-8, e.g. "com/example/MainActivity")
 *     4 bytes  — dex bytes length
 *     M bytes  — dex file bytes
 *
 * The SwapAgent calls JVMTI RedefineClasses() with the received bytecode.
 * Returns false when the swap was rejected (new field/class added — requires full reinstall).
 */
class SwapAgentClient(private val port: Int = 7777) {

    fun isAgentReachable(): Boolean {
        return try {
            Socket("localhost", port).use { true }
        } catch (_: ConnectException) { false }
        catch (_: Exception) { false }
    }

    /**
     * Sends all .dex files produced by the latest build to the SwapAgent.
     * Returns SwapResult indicating success or whether a full reinstall is needed.
     */
    fun pushDex(dexFiles: List<File>): SwapResult {
        if (dexFiles.isEmpty()) return SwapResult.NoDexFiles

        return try {
            Socket("localhost", port).use { socket ->
                socket.soTimeout = 10_000
                val out = DataOutputStream(socket.getOutputStream())

                out.writeInt(dexFiles.size)
                for (dex in dexFiles) {
                    val name = dex.nameWithoutExtension
                    val nameBytes = name.toByteArray(Charsets.UTF_8)
                    val dexBytes = dex.readBytes()
                    out.writeShort(nameBytes.size)
                    out.write(nameBytes)
                    out.writeInt(dexBytes.size)
                    out.write(dexBytes)
                }
                out.flush()

                val response = socket.getInputStream().read()
                when (response) {
                    0 -> SwapResult.Success
                    // SwapAgent returns 1 when RedefineClasses rejects structural change
                    1 -> SwapResult.StructuralChange
                    else -> SwapResult.AgentError("Unexpected response code: $response")
                }
            }
        } catch (e: ConnectException) {
            SwapResult.AgentNotReachable
        } catch (e: Exception) {
            SwapResult.AgentError(e.message ?: "Unknown socket error")
        }
    }
}

sealed class SwapResult {
    object Success : SwapResult()
    // New field, class, or method signature changed — JVMTI cannot hot-swap, need full reinstall
    object StructuralChange : SwapResult()
    object AgentNotReachable : SwapResult()
    object NoDexFiles : SwapResult()
    data class AgentError(val reason: String) : SwapResult()
}
