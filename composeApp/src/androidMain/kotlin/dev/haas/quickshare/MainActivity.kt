package dev.haas.quickshare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import dev.haas.quickshare.core.models.PermissionInfo

class MainActivity : ComponentActivity() {

    private var permissionRefreshTick by mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Trigger recomposition so UI shows updated statuses
        permissionRefreshTick++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Request permissions immediately on first launch
        permissionLauncher.launch(PermissionHelper.getPermissionsToRequest())

        setContent {
            App(
                permissionRefreshTick = permissionRefreshTick,
                permissions = buildPermissions(),
                onRequestPermissions = {
                    permissionLauncher.launch(PermissionHelper.getPermissionsToRequest())
                },
                onOpenBatterySettings = {
                    try {
                        startActivity(
                            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        )
                    } catch (e: Exception) {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                },
                onOpenAppSettings = {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                    )
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check every time the user returns (e.g. from Settings)
        permissionRefreshTick++
    }

    private fun buildPermissions(): List<PermissionInfo> =
        PermissionHelper.getPermissionStatuses(this).map {
            PermissionInfo(
                label = it.label,
                description = it.description,
                isGranted = it.isGranted
            )
        }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}