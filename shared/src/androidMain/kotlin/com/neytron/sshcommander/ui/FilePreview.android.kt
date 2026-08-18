package com.neytron.sshcommander.ui

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** Android image decoder — BitmapFactory + asImageBitmap(). */
internal actual fun decodePreviewImage(bytes: ByteArray): ImageBitmap? =
    runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
