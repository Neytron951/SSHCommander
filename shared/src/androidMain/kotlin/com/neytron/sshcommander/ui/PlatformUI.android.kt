package com.neytron.sshcommander.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Set once by androidApp's Application/MainActivity so shared code can toast. */
object AndroidAppContext {
    var appContext: Context? = null
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun PlatformAdBanner(blockId: String) {
    // Ad banner is rendered by androidApp (NativeAdBanner) via a slot;
    // this actual is only used when the shared UI is run without ads.
}

@Composable
actual fun isLandscapeLayout(): Boolean =
    LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

actual fun platformToast(message: String) {
    AndroidAppContext.appContext?.let { ctx ->
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
    }
}
