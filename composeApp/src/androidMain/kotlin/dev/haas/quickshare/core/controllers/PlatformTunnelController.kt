package dev.haas.quickshare.core.controllers

import dev.haas.quickshare.ssh.SshReverseTunnelManager

actual class PlatformTunnelController actual constructor() {
    private var tunnelManager: SshReverseTunnelManager? = null
    private var _isRunning: Boolean = false
    actual val isRunning: Boolean get() = _isRunning

    actual suspend fun start(localPort: Int, onLog: (String) -> Unit, onUrlMapped: (String) -> Unit) {
        stop()
        tunnelManager = SshReverseTunnelManager(
            onLog = onLog,
            onUrlAssigned = onUrlMapped
        )
        tunnelManager?.startTunnel()
        _isRunning = true
    }

    actual suspend fun stop() {
        tunnelManager?.stopTunnel()
        tunnelManager = null
        _isRunning = false
    }
}
