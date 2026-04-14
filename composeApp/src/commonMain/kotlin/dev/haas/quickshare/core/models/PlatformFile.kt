package dev.haas.quickshare.core.models

interface PlatformFile {
    val name: String
    val size: Long
    val uriString: String
    suspend fun readBytes(): ByteArray
}
