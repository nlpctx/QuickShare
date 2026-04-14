package dev.haas.quickshare.core.controllers

import kotlinx.cinterop.*
import platform.posix.*
import platform.darwin.*

actual class PlatformTunnelController actual constructor() {
    private var _isRunning: Boolean = false
    actual val isRunning: Boolean get() = _isRunning

    actual suspend fun start(localPort: Int, onLog: (String) -> Unit, onUrlMapped: (String) -> Unit) {
        val ip = getLocalIPAddress() ?: "localhost"
        onLog("Tunneling is restricted on iOS. Using local network address: $ip")
        onLog("Note: Recipients must be on the same Wi-Fi network.")
        onUrlMapped("http://$ip:$localPort")
        _isRunning = true
    }

    actual suspend fun stop() {
        _isRunning = false
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getLocalIPAddress(): String? {
        var address: String? = null
        memScoped {
            val ifaddrPtr = alloc<CPointerVar<ifaddrs>>()
            if (getifaddrs(ifaddrPtr.ptr) == 0) {
                var curr = ifaddrPtr.value
                while (curr != null) {
                    val interfaceData = curr.pointed
                    val name = interfaceData.ifa_name?.toKString()
                    val family = interfaceData.ifa_addr?.pointed?.sa_family?.toInt()
                    
                    // AF_INET is usually 2 on iOS
                    if (family == AF_INET.toInt()) {
                        if (name == "en0") { // Wi-Fi interface
                            val sin = interfaceData.ifa_addr!!.reinterpret<sockaddr_in>().pointed
                            val addr = inet_ntoa(sin.sin_addr)
                            address = addr?.toKString()
                            if (address != "127.0.0.1") break
                        }
                    }
                    curr = interfaceData.ifa_next
                }
                freeifaddrs(ifaddrPtr.value)
            }
        }
        return address
    }
}
