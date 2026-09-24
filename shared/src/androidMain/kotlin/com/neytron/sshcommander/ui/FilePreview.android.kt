package com.neytron.sshcommander.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun decodePreviewImage(bytes: ByteArray): ImageBitmap? =
    runCatching {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()

internal actual fun decodeScreenImage(bytes: ByteArray, maxWidth: Int, maxHeight: Int): ImageBitmap? =
    runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        val w = options.outWidth
        val h = options.outHeight

        var inSampleSize = 1
        if (w > maxWidth || h > maxHeight) {
            val halfW = w / 2
            val halfH = h / 2
            while ((halfW / inSampleSize) >= maxWidth && (halfH / inSampleSize) >= maxHeight) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return@runCatching null

        if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
            val scale = minOf(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
            val newW = (bitmap.width * scale).toInt()
            val newH = (bitmap.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            if (scaled != bitmap) bitmap.recycle()
            scaled.asImageBitmap()
        } else {
            bitmap.asImageBitmap()
        }
    }.getOrNull()
