package com.adhithimaran.cultivate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = CultivatePrimaryLight,
    onPrimary        = CultivateOnPrimary,
    secondary        = CultivateSecondaryLight,
    onSecondary      = CultivateOnSecondary,
    tertiary         = CultivateTertiaryLight,
    onTertiary       = CultivateOnTertiary,
    background       = CultivateBackgroundDark,
    onBackground     = CultivateOnBackgroundDark,
    surface          = CultivateSurfaceDark,
    onSurface        = CultivateOnSurfaceDark,
    surfaceVariant   = CultivateSurfaceVariantDark,
    onSurfaceVariant = CultivateOnSurfaceVariant,
    outline          = CultivateOutlineDark,
    error            = CultivateErrorDark,
    onError          = CultivateOnErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary          = CultivatePrimary,
    onPrimary        = CultivateOnPrimary,
    secondary        = CultivateSecondary,
    onSecondary      = CultivateOnSecondary,
    tertiary         = CultivateTertiary,
    onTertiary       = CultivateOnTertiary,
    background       = CultivateBackground,
    onBackground     = CultivateOnBackground,
    surface          = CultivateSurface,
    onSurface        = CultivateOnSurface,
    surfaceVariant   = CultivateSurfaceVariant,
    onSurfaceVariant = CultivateOnSurfaceVariant,
    outline          = CultivateOutline,
    error            = CultivateError,
    onError          = CultivateOnError,
)

@Composable
fun CultivateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false so your custom palette always shows instead of the wallpaper-based one
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Tint the status bar to match the background
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}