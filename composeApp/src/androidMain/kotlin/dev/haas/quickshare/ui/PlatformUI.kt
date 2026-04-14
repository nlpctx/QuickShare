package dev.haas.quickshare.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.haas.quickshare.core.AndroidContext
import dev.haas.quickshare.core.models.AndroidPlatformFile
import dev.haas.quickshare.core.models.PlatformFile
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia

actual object PlatformUI {
    @Composable
    actual fun rememberFilePicker(onResult: (List<PlatformFile>) -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    println("Could not take persistable permission for $uri: ${e.message}")
                }
            }
            onResult(uris.map { AndroidPlatformFile(context, it) })
        }
        return {
            launcher.launch(arrayOf("*/*"))
        }
    }

    @Composable
    actual fun rememberImagePicker(onResult: (List<PlatformFile>) -> Unit): () -> Unit {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) { uris ->
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) {
                    println("Could not take persistable permission for $uri: ${e.message}")
                }
            }
            onResult(uris.map { AndroidPlatformFile(context, it) })
        }
        return {
            launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
        }
    }

    actual fun openBatterySettings() {
        val context = AndroidContext.get()
        val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to app details
            val detailIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(detailIntent)
        }
    }

    actual fun createPlatformFile(uriString: String): PlatformFile? {
        return try {
            val context = AndroidContext.get()
            val uri = android.net.Uri.parse(uriString)
            AndroidPlatformFile(context, uri)
        } catch (e: Exception) {
            null
        }
    }

    actual fun requestPermissions() {
        // This is tricky from a static object, usually done via Activity
        // But we can send a broadcast or use a helper activity
        // For now, we'll guide the user in the UI
    }
    
    actual fun shareLink(url: String) {
        val context = AndroidContext.get()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Link").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    
    actual fun copyToClipboard(text: String) {
        val context = AndroidContext.get()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QuickShare Link", text)
        clipboard.setPrimaryClip(clip)
        showToast("Copied to clipboard")
    }
    
    actual fun showToast(message: String) {
        val context = AndroidContext.get()
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @Composable
    actual fun generateQRCode(content: String): ImageBitmap {
        return remember(content) {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap.asImageBitmap()
        }
    }

    actual suspend fun loadThumbnail(file: PlatformFile): ImageBitmap? {
        if (file !is AndroidPlatformFile) return null
        return withContext(Dispatchers.IO) {
            try {
                file.context.contentResolver.openInputStream(file.uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    // We must mark and reset or reopen the stream since decodeStream consumes it.
                    // Instead, reopen it:
                }
                
                // Real decoding
                file.context.contentResolver.openInputStream(file.uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // Subsample to save memory 
                    }
                    val bitmap = BitmapFactory.decodeStream(stream, null, options)
                    bitmap?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    actual fun savePersistentData(key: String, data: String) {
        val context = AndroidContext.get()
        val prefs = context.getSharedPreferences("quickshare_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(key, data).apply()
    }

    actual fun loadPersistentData(key: String): String? {
        val context = AndroidContext.get()
        val prefs = context.getSharedPreferences("quickshare_prefs", Context.MODE_PRIVATE)
        return prefs.getString(key, null)
    }

    actual fun logEvent(name: String, properties: Map<String, String>) {
        try {
            val propertiesMap = properties.entries.associate { it.key to it.value }
            com.posthog.PostHog.capture(name, properties = propertiesMap)
        } catch (e: Exception) {
            println("PostHog Log Error: ${e.message}")
        }
    }
}
