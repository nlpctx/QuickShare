package dev.haas.quickshare.core.controllers

import dev.haas.quickshare.core.models.PlatformFile

expect class PlatformServerController() {
    suspend fun start(port: Int, files: List<PlatformFile>, token: String)
    suspend fun stop()
    fun addShare(files: List<PlatformFile>, token: String)
    fun removeShare(token: String)
    val isRunning: Boolean
}

expect class PlatformTunnelController() {
    suspend fun start(localPort: Int, onLog: (String) -> Unit, onUrlMapped: (String) -> Unit)
    suspend fun stop()
    val isRunning: Boolean
}
