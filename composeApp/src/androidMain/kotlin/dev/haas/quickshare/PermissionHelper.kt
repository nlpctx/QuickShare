package dev.haas.quickshare

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {

    data class PermissionStatus(
        val name: String,
        val label: String,
        val description: String,
        val isGranted: Boolean
    )

    fun getPermissionStatuses(context: Context): List<PermissionStatus> {
        val statuses = mutableListOf<PermissionStatus>()

        // INTERNET - always granted, no runtime request needed
        statuses.add(
            PermissionStatus(
                name = Manifest.permission.INTERNET,
                label = "Internet",
                description = "Required to create the sharing tunnel",
                isGranted = true
            )
        )

        // POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            statuses.add(
                PermissionStatus(
                    name = Manifest.permission.POST_NOTIFICATIONS,
                    label = "Notifications",
                    description = "Allows showing a persistent notification while sharing",
                    isGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        }

        // READ_MEDIA_IMAGES / READ_MEDIA_VIDEO (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val imagesGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val videoGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
            statuses.add(
                PermissionStatus(
                    name = Manifest.permission.READ_MEDIA_IMAGES,
                    label = "Photos & Videos",
                    description = "Allows selecting photos and videos to share",
                    isGranted = imagesGranted && videoGranted
                )
            )
        } else {
            // Legacy storage permission
            statuses.add(
                PermissionStatus(
                    name = Manifest.permission.READ_EXTERNAL_STORAGE,
                    label = "Storage",
                    description = "Allows reading photos from storage",
                    isGranted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                )
            )
        }

        return statuses
    }

    fun getPermissionsToRequest(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}
