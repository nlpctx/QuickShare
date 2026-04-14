package dev.haas.quickshare.core.models

import kotlinx.serialization.Serializable

@Serializable
data class SharedPhotoInfo(
    val id: String,
    val name: String,
    val size: Long,
    val thumbnailUrl: String,
    val downloadUrl: String
)
