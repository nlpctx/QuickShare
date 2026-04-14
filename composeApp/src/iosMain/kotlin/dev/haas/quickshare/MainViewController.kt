package dev.haas.quickshare

import androidx.compose.ui.window.ComposeUIViewController
import dev.haas.quickshare.core.AppState
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
