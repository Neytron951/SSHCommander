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

// Storm Tokyo Night Palette: Сбалансированная тёмная тема без лишнего неона.
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7AA2F7),
    onPrimary = Color(0xFF1A1B26),
    primaryContainer = Color(0xFF2C335E),
    onPrimaryContainer = Color(0xFFC0CAF5),
    secondary = Color(0xFFBB9AF7),
    onSecondary = Color(0xFF1A1B26),
    secondaryContainer = Color(0xFF333856),
    onSecondaryContainer = Color(0xFFC0CAF5),
    tertiary = Color(0xFF9ECE6A),
    onTertiary = Color(0xFF1A1B26),
    background = Color(0xFF1A1B26),
    onBackground = Color(0xFFC0CAF5),
    surface = Color(0xFF1F2335),
    onSurface = Color(0xFFC0CAF5),
    surfaceVariant = Color(0xFF24283B),
    onSurfaceVariant = Color(0xFFA9B1D6),
    outline = Color(0xFF565F89)
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
