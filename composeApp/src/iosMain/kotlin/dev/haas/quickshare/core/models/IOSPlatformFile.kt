package dev.haas.quickshare.core.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IOSPlatformFile(
    override val name: String,
    override val size: Long,
    private val data: ByteArray? = null
) : PlatformFile {
    override val uriString: String get() = "ios://$name"
    
    override suspend fun readBytes(): ByteArray = withContext(Dispatchers.Default) {
        data ?: ByteArray(0)
    }
}
