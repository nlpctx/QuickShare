package dev.haas.quickshare.core.models

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val id: String,
    val name: String,
    val fileUris: List<String>,
    val fileNames: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)
