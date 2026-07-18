package com.forgemind.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val ForgeMindDarkColors = darkColorScheme(
    primary = PrimaryBlue,
    secondary = AccentCyan,
    tertiary = SuccessGreen,

    background = BackgroundDark,
    surface = SurfaceDark,

    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,

    onBackground = TextPrimary,
    onSurface = TextPrimary,

    error = CriticalRed
)

private val ForgeMindShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ForgeMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ForgeMindDarkColors,
        typography = ForgeMindTypography,
        shapes = ForgeMindShapes,
        content = content
    )
}