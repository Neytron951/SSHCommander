package com.neytron.sshcommander.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix
import java.util.Random

@Composable
actual fun QRCodeImage(
    content: String,
    modifier: Modifier,
    size: Dp,
    color: Color,
    backgroundColor: Color
) {
    val bitMatrix = remember(content) {
        try {
            val writer = QRCodeWriter()
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
        } catch (e: Exception) {
            null
        }
    }

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            if (bitMatrix != null) {
                val matrixSize = bitMatrix.width
                val pixelSize = this.size.width / matrixSize
                
                drawRect(color = backgroundColor, size = this.size)

                for (y in 0 until matrixSize) {
                    for (x in 0 until matrixSize) {
                        if (bitMatrix.get(x, y)) {
                            drawRect(
                                color = color,
                                topLeft = Offset(x * pixelSize, y * pixelSize),
                                size = Size(pixelSize + 0.5f, pixelSize + 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
