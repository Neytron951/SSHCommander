package com.neytron.sshcommander.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

internal actual fun decodePreviewImage(bytes: ByteArray): ImageBitmap? =
    runCatching {
        ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap()
    }.getOrNull()

internal actual fun decodeScreenImage(bytes: ByteArray, maxWidth: Int, maxHeight: Int): ImageBitmap? =
    runCatching {
        val original = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@runCatching null
        val w = original.width
        val h = original.height
        if (w <= maxWidth && h <= maxHeight) {
            original.toComposeImageBitmap()
        } else {
            val scale = minOf(maxWidth.toDouble() / w, maxHeight.toDouble() / h)
            val newW = (w * scale).toInt().coerceAtLeast(100)
            val newH = (h * scale).toInt().coerceAtLeast(100)
            val resized = BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB)
            val g = resized.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(original, 0, 0, newW, newH, null)
            g.dispose()
            resized.toComposeImageBitmap()
        }
    }.getOrNull()
