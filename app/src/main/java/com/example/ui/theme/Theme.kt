package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val TailorLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = GoldSecondary,
    onSecondary = GoldOnSecondary,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = GoldOnContainer,
    tertiary = SlateTertiary,
    onTertiary = SlateOnTertiary,
    tertiaryContainer = SlateContainer,
    onTertiaryContainer = SlateOnContainer,
    background = LinenBackground,
    onBackground = LinenOnBackground,
    surface = LinenSurface,
    onSurface = LinenOnSurface,
    surfaceVariant = LinenSurfaceVariant,
    onSurfaceVariant = LinenOnSurfaceVariant,
    surfaceContainerLowest = LinenSurfaceContainerLowest,
    surfaceContainerLow = LinenSurfaceContainerLow,
    surfaceContainer = LinenSurfaceContainer,
    surfaceContainerHigh = LinenSurfaceContainerHigh,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val TailorDarkColorScheme = darkColorScheme(
    primary = EmeraldDarkPrimary,
    onPrimary = EmeraldDarkOnPrimary,
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = EmeraldDarkOnContainer,
    secondary = GoldDarkSecondary,
    onSecondary = GoldDarkOnSecondary,
    secondaryContainer = GoldDarkContainer,
    onSecondaryContainer = GoldDarkOnContainer,
    tertiary = SlateDarkTertiary,
    onTertiary = SlateDarkOnTertiary,
    tertiaryContainer = SlateDarkContainer,
    onTertiaryContainer = SlateDarkOnContainer,
    background = CharcoalBackground,
    onBackground = CharcoalOnBackground,
    surface = CharcoalSurface,
    onSurface = CharcoalOnSurface,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CharcoalOnSurfaceVariant,
    surfaceContainerLowest = CharcoalSurfaceContainerLowest,
    surfaceContainerLow = CharcoalSurfaceContainerLow,
    surfaceContainer = CharcoalSurfaceContainer,
    surfaceContainerHigh = CharcoalSurfaceContainerHigh,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

val TailorShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TailorBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TailorDarkColorScheme
        else -> TailorLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TailorTypography,
        shapes = TailorShapes,
        content = content
    )
}
