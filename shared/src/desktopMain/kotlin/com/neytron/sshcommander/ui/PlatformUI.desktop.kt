package com.neytron.sshcommander.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop: no system back button. Esc/arrows are handled in the UI itself.
}

@Composable
actual fun PlatformAdBanner(blockId: String) {
    // Desktop: no ads.
}

@Composable
actual fun isLandscapeLayout(): Boolean = true

actual fun platformToast(message: String) {
    println("[SSH Commander] $message")
}
