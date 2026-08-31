package com.neytron.sshcommander.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    NativeAdBanner(blockId = blockId)
}

@Composable
actual fun isLandscapeLayout(): Boolean =
    LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

actual fun platformToast(message: String) {
    AndroidAppContext.appContext?.let { ctx ->
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
    }
}

actual fun platformOpenUrl(url: String) {
    AndroidAppContext.appContext?.let { ctx ->
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }
}

@Composable
actual fun getSystemFontFamily(name: String): androidx.compose.ui.text.font.FontFamily {
    return androidx.compose.runtime.remember(name) {
        when (name) {
            "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
            "JetBrains Mono" -> androidx.compose.ui.text.font.FontFamily.Monospace // Defaulting to system mono for now
            else -> try {
                androidx.compose.ui.text.font.FontFamily(android.graphics.Typeface.create(name, android.graphics.Typeface.NORMAL))
            } catch (e: Exception) {
                androidx.compose.ui.text.font.FontFamily.Monospace
            }
        }
    }
}

@Composable
actual fun Modifier.platformDragAndDrop(onFilesDropped: (List<String>) -> Unit): Modifier = this
