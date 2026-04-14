package dev.haas.quickshare.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QRCodeView(content: String, modifier: Modifier = Modifier.size(200.dp).padding(16.dp)) {
    val bitmap = PlatformUI.generateQRCode(content)
    Image(
        bitmap = bitmap,
        contentDescription = "QR Code",
        modifier = modifier
    )
}
