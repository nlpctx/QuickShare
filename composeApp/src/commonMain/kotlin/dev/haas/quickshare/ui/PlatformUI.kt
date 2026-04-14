package dev.haas.quickshare.ui

import androidx.compose.runtime.Composable
import dev.haas.quickshare.core.models.PlatformFile

expect object PlatformUI {
    @Composable
    fun rememberFilePicker(onResult: (List<PlatformFile>) -> Unit): () -> Unit

    @Composable
    fun rememberImagePicker(onResult: (List<PlatformFile>) -> Unit): () -> Unit
    
    fun shareLink(url: String)
    
    fun copyToClipboard(text: String)
    
    fun showToast(message: String)

    fun openBatterySettings()

    fun createPlatformFile(uriString: String): PlatformFile?

    fun requestPermissions()

    @Composable
    fun generateQRCode(content: String): androidx.compose.ui.graphics.ImageBitmap

    suspend fun loadThumbnail(file: PlatformFile): androidx.compose.ui.graphics.ImageBitmap?

    fun savePersistentData(key: String, data: String)


    fun loadPersistentData(key: String): String?

    fun logEvent(name: String, properties: Map<String, String> = emptyMap())
}
