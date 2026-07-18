package com.forgemind.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun ForgeMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ForgeMindDarkColors,
        typography = ForgeMindTypography,
        content = content
    )
}