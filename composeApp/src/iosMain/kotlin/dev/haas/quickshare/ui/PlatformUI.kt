package dev.haas.quickshare.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.interop.LocalUIViewController
import dev.haas.quickshare.core.models.IOSPlatformFile
import dev.haas.quickshare.core.models.PlatformFile
import platform.PhotosUI.*
import platform.UIKit.*
import platform.Foundation.*
import platform.CoreImage.*
import platform.darwin.NSObject
import kotlinx.cinterop.*
import platform.posix.memcpy
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import platform.darwin.DISPATCH_TIME_NOW

actual object PlatformUI {
    @Composable
    actual fun rememberFilePicker(onResult: (List<PlatformFile>) -> Unit): () -> Unit {
        val uiViewController = LocalUIViewController.current
        
        val delegate = remember {
            object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                    picker.dismissViewControllerAnimated(true, null)
                    
                    val results = didFinishPicking.filterIsInstance<PHPickerResult>()
                    if (results.isEmpty()) {
                        onResult(emptyList())
                        return
                    }
                    
                    val files = mutableListOf<PlatformFile>()
                    var processedCount = 0
                    
                    results.forEach { result ->
                        val itemProvider = result.itemProvider
                        if (itemProvider.canLoadObjectOfClass(UIImage) || itemProvider.hasItemConformingToTypeIdentifier("public.movie")) {
                            val typeIdentifier = if (itemProvider.canLoadObjectOfClass(UIImage)) "public.image" else "public.movie"
                            itemProvider.loadDataRepresentationForTypeIdentifier(typeIdentifier) { data, error ->
                                if (data != null) {
                                    val extension = if (typeIdentifier == "public.image") "jpg" else "mp4"
                                    val fileName = "file_${NSUUID().UUIDString}.$extension"
                                    val byteArray = data.toByteArray()
                                    files.add(IOSPlatformFile(fileName, data.length.toLong(), byteArray))
                                }
                                
                                processedCount++
                                if (processedCount == results.size) {
                                    onResult(files)
                                }
                            }
                        } else {
                            processedCount++
                        }
                    }
                }
            }
        }

        return {
            val configuration = PHPickerConfiguration()
            configuration.setSelectionLimit(50)
            configuration.setFilter(PHPickerFilter.anyFilterWithSubfilters(listOf(PHPickerFilter.imagesFilter(), PHPickerFilter.videosFilter())))
            
            val picker = PHPickerViewController(configuration)
            picker.delegate = delegate
            uiViewController.presentViewController(picker, true, null)
        }
    }
    
    @Composable
    actual fun rememberImagePicker(onResult: (List<PlatformFile>) -> Unit): () -> Unit {
        return rememberFilePicker(onResult)
    }
    
    actual fun shareLink(url: String) {
        val activityItems = listOf(url)
        val activityController = UIActivityViewController(activityItems, null)
        
        val window = UIApplication.sharedApplication.windows.firstOrNull { (it as UIWindow).isKeyWindow() } as? UIWindow
        window?.rootViewController?.presentViewController(activityController, true, null)
    }
    
    actual fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
        showToast("Copied to clipboard")
    }
    
    actual fun showToast(message: String) {
        val alert = UIAlertController.alertControllerWithTitle(null, message, UIAlertControllerStyleAlert)
        val window = UIApplication.sharedApplication.windows.firstOrNull { (it as UIWindow).isKeyWindow() } as? UIWindow
        window?.rootViewController?.let { root ->
            root.presentViewController(alert, true, null)
            val delay = 2.0 * platform.Foundation.NSEC_PER_SEC.toDouble()
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delay.toLong()), dispatch_get_main_queue()) {
                alert.dismissViewControllerAnimated(true, null)
            }
        }
    }

    actual fun openBatterySettings() {
    }

    actual fun createPlatformFile(uriString: String): PlatformFile? {
        return null
    }

    actual fun requestPermissions() {
    }
    @Composable
    actual fun generateQRCode(content: String): ImageBitmap {
        return remember(content) {
            val filter = CIFilter.filterWithName("CIQRCodeGenerator")
            filter?.setValue((content as NSString).dataUsingEncoding(NSUTF8StringEncoding), forKey = "inputMessage")
            filter?.setValue("H", forKey = "inputCorrectionLevel")
            
            val outputImage = filter?.outputImage
            if (outputImage != null) {
                val context = CIContext.contextWithOptions(null)
                val cgImage = context.createCGImage(outputImage, outputImage.extent)
                if (cgImage != null) {
                    val uiImage = UIImage.imageWithCGImage(cgImage)
                    val data = UIImagePNGRepresentation(uiImage)
                    if (data != null) {
                        val bytes = data.toByteArray()
                        org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    } else {
                        ImageBitmap(1, 1)
                    }
                } else {
                    ImageBitmap(1, 1)
                }
            } else {
                ImageBitmap(1, 1)
            }
        }
    }

    actual suspend fun loadThumbnail(file: PlatformFile): ImageBitmap? {
        return try {
            val bytes = file.readBytes()
            org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    actual fun savePersistentData(key: String, data: String) {
        NSUserDefaults.standardUserDefaults.setObject(data, key)
    }

    actual fun loadPersistentData(key: String): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(key)
    }
}

@OptIn(UnsafeNumber::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val byteArray = ByteArray(size)
    if (size > 0) {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return byteArray
}
