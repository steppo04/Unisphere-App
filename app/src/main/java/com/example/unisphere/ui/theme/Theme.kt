package com.example.unisphere.ui.theme

import android.os.Build
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    secondary = SecondaryTealDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    tertiary = TertiaryCoralDark,
    onTertiary = OnTertiaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    secondary = SecondaryTealLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    tertiary = TertiaryCoralLight,
    onTertiary = OnTertiaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight
)

@Composable
fun UniSphereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SideEffect {
        Log.d(
            "THEME_DEBUG",
            "UniSphereTheme riceve darkTheme=$darkTheme"
        )
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    SideEffect {
        Log.d(
            "THEME_DEBUG",
            "Palette applicata: ${
                if (darkTheme) "DarkColorScheme" else "LightColorScheme"
            }"
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}