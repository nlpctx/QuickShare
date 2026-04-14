package dev.haas.quickshare.core.models

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPlatformFile(
    val context: Context,
    val uri: Uri
) : PlatformFile {
    override val uriString: String get() = uri.toString()

    override val name: String by lazy {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) result = cursor.getString(index)
                    }
                }
            } catch (e: SecurityException) {
                println("Permission lost for URI: $uri")
            }
        }
        result ?: (uri.path?.split('/')?.lastOrNull() ?: "unknown")
    }

    override val size: Long by lazy {
        var result: Long = 0
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (index >= 0) result = cursor.getLong(index)
                    }
                }
            } catch (e: SecurityException) {
                // Return 0 if permission lost
            }
        }
        result
    }

    override suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        } catch (e: SecurityException) {
            ByteArray(0)
        }
    }
}
