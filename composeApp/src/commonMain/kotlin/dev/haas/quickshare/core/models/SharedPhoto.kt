package dev.haas.quickshare.core.models

data class SharedPhoto(
    val id: String,
    val name: String,
    val size: Long,
    val file: PlatformFile
)
