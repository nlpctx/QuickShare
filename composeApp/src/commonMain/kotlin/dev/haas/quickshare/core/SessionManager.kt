package dev.haas.quickshare.core

import dev.haas.quickshare.core.controllers.PlatformServerController
import dev.haas.quickshare.core.controllers.PlatformTunnelController
import dev.haas.quickshare.core.models.ActiveShare
import dev.haas.quickshare.core.models.PlatformFile
import dev.haas.quickshare.core.security.TokenGenerator
import dev.haas.quickshare.ui.PlatformUI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SessionManager(
    private val scope: CoroutineScope,
    private val serverController: PlatformServerController,
    private val tunnelController: PlatformTunnelController
) {
    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _tunnelUrl = MutableStateFlow<String?>(null)
    val tunnelUrl: StateFlow<String?> = _tunnelUrl.asStateFlow()

    private val _activeShares = MutableStateFlow<List<ActiveShare>>(emptyList())
    val activeShares: StateFlow<List<ActiveShare>> = _activeShares.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _secondsRunning = MutableStateFlow(0)
    val secondsRunning: StateFlow<Int> = _secondsRunning.asStateFlow()

    private var timerJob: Job? = null

    fun appendLog(message: String) {
        _logs.value = _logs.value + message
    }

    fun startSession(files: List<PlatformFile>) {
        if (_isSharing.value) return
        
        val token = TokenGenerator.generate()
        _activeShares.value = listOf(ActiveShare(token, files.size))
        _isSharing.value = true
        _secondsRunning.value = 0
        
        scope.launch {
            try {
                PlatformUI.logEvent("session_started", mapOf("file_count" to files.size.toString()))
                appendLog("Starting local server...")
                serverController.start(8080, files, token)
                
                appendLog("Starting tunnel...")
                tunnelController.start(8080, 
                    onLog = { appendLog(it) },
                    onUrlMapped = { _tunnelUrl.value = it }
                )
                
                startTimer()
            } catch (e: Exception) {
                appendLog("Failed to start session: ${e.message}")
                stopSession()
            }
        }
    }

    fun createAdditionalShare(files: List<PlatformFile>): String {
        val token = TokenGenerator.generate()
        serverController.addShare(files, token)
        _activeShares.value = _activeShares.value + ActiveShare(token, files.size)
        return token
    }

    fun stopShare(token: String) {
        serverController.removeShare(token)
        val updatedShares = _activeShares.value.filter { it.token != token }
        _activeShares.value = updatedShares
        
        if (updatedShares.isEmpty()) {
            stopSession("Last share link stopped.")
        } else {
            appendLog("Stopped sharing link with token: $token")
        }
    }

    fun stopSession(reason: String? = null) {
        if (!_isSharing.value) return
        
        scope.launch {
            tunnelController.stop()
            serverController.stop()
            _isSharing.value = false
            _tunnelUrl.value = null
            _activeShares.value = emptyList()
            stopTimer()
            if (reason != null) {
                appendLog("Session stopped: $reason")
            } else {
                appendLog("Session stopped.")
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                _secondsRunning.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}
