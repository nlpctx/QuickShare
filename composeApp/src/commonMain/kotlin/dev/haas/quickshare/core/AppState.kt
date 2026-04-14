package dev.haas.quickshare.core

import dev.haas.quickshare.core.models.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import dev.haas.quickshare.core.controllers.PlatformServerController
import dev.haas.quickshare.core.controllers.PlatformTunnelController

object AppState {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    val sessionManager = SessionManager(
        scope = scope,
        serverController = PlatformServerController(),
        tunnelController = PlatformTunnelController()
    )

    private val _selectedFiles = MutableStateFlow<List<PlatformFile>>(emptyList())
    val selectedFiles: StateFlow<List<PlatformFile>> = _selectedFiles.asStateFlow()

    fun setSelectedFiles(files: List<PlatformFile>) {
        _selectedFiles.value = files
    }

    val albumManager = AlbumManager
}
