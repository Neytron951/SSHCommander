package com.neytron.sshcommander.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/** Desktop (Skia) image decoder — reads the encoded bytes via ImageIO. */
internal actual fun decodePreviewImage(bytes: ByteArray): ImageBitmap? =
    runCatching {
        ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap()
    }.getOrNull()
