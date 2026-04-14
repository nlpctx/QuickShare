package dev.haas.quickshare.core.models

data class PermissionInfo(
    val label: String,
    val description: String,
    val isGranted: Boolean,
    val isRequired: Boolean = true
)
