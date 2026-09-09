package com.neytron.sshcommander.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun QRCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    color: Color = Color.Black,
    backgroundColor: Color = Color.White
)
