package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = PrimaryAction,
    onPrimary = Color.White,
    primaryContainer = CloudSeaBlue,
    onPrimaryContainer = PrimaryText,
    secondary = DeepSeaSaltBlue,
    onSecondary = Color.White,
    secondaryContainer = LightSeaBlue,
    onSecondaryContainer = PrimaryText,
    tertiary = SuccessText,
    onTertiary = Color.White,
    tertiaryContainer = LightMintGreen,
    onTertiaryContainer = SuccessText,
    background = PageBackground,
    onBackground = PrimaryText,
    surface = WhiteCard,
    onSurface = PrimaryText,
    surfaceVariant = CloudSeaBlue,
    onSurfaceVariant = SecondaryText,
    outline = BorderStroke,
    outlineVariant = LightSeaBlue,
    error = ErrorText,
    onError = Color.White,
    errorContainer = ErrorLightSurface,
    onErrorContainer = ErrorText
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimaryAction,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkPrimaryText,
    secondary = SeaSaltBlue,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkPrimaryText,
    tertiary = DarkSuccess,
    onTertiary = DarkOnPrimary,
    tertiaryContainer = Color(0xFF19352F),
    onTertiaryContainer = DarkSuccess,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkSurface,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkBorderStroke,
    outlineVariant = DarkBorderStroke,
    error = DarkError,
    onError = DarkOnPrimary,
    errorContainer = DarkErrorSurface,
    onErrorContainer = DarkError
)

val SqlLearningShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun SqlLearningTheme(
    appThemeMode: String = "light",
    content: @Composable () -> Unit
) {
    val darkTheme = when (appThemeMode) {
        "dark" -> true
        "system" -> isSystemInDarkTheme()
        else -> false
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SqlLearningTypography,
        shapes = SqlLearningShapes,
        content = content
    )
}
