package com.neytron.sshcommander.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Синяя палитра (Material 3 "Blue"), фирменный цвет SSH Commander.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    onPrimary = Color(0xFF003257),
    primaryContainer = Color(0xFF00497C),
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = Color(0xFF66D0F0),
    onSecondary = Color(0xFF003544),
    secondaryContainer = Color(0xFF004E62),
    onSecondaryContainer = Color(0xFFB8EAFF),
    tertiary = Color(0xFFA9C8FF),
    onTertiary = Color(0xFF0A315F),
    tertiaryContainer = Color(0xFF1D4879),
    onTertiaryContainer = Color(0xFFD2E4FF),
    background = Color(0xFF0F141A),
    onBackground = Color(0xFFDEE3EB),
    surface = Color(0xFF0F141A),
    onSurface = Color(0xFFDEE3EB),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9199)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF001D34),
    secondary = Color(0xFF006782),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFB8EAFF),
    onSecondaryContainer = Color(0xFF001F2A),
    tertiary = Color(0xFF316295),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD2E4FF),
    onTertiaryContainer = Color(0xFF001D3B),
    background = Color(0xFFF8FDFF),
    onBackground = Color(0xFF001F2A),
    surface = Color(0xFFF8FDFF),
    onSurface = Color(0xFF001F2A),
    surfaceVariant = Color(0xFFD6E3FF), // Более холодный светло-синий оттенок
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF72777F)
)

/**
 * Cross-platform theme for SSH Commander. Preserves the Android palette and
 * typography; dynamic color is Android-only so it is omitted here.
 */
@Composable
fun SSHCommanderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val customTypography = Typography(
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 18.sp),
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 14.sp),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content
    )
}
