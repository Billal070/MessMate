package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.local.ThemeMode
import com.example.ui.components.LocalReducedMotion
import com.example.ui.components.rememberSystemReducedMotion

@Immutable
data class MessMateExtraColors(
    val isDark: Boolean,
    val divider: Color,
    val borderStrong: Color,
    val success: Color,
    val successContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val surfaceLow: Color,
    val surfaceElevated: Color,
    val primarySoft: Color,
    val primarySoftBorder: Color,
    val amber: Color,
    val amberContainer: Color,
    val amberBorder: Color,
    val flameStart: Color,
    val flameMid: Color,
    val flameEnd: Color,
    val greenStart: Color,
    val greenEnd: Color,
    val heroBgTop: Color,
    val heroBgBottom: Color,
    val mealBlue: Color,
    val studyGreen: Color,
    val riceOrange: Color,
    val neutralGrey: Color
)

private val LightExtraColors = MessMateExtraColors(
    isDark = false,
    divider = WarmDivider,
    borderStrong = WarmOutlineStrong,
    success = WarmEmerald,
    successContainer = WarmEmeraldContainer,
    danger = WarmDanger,
    dangerContainer = WarmDangerContainer,
    warning = WarmWarning,
    warningContainer = WarmWarningContainer,
    surfaceLow = WarmSurfaceLow,
    surfaceElevated = WarmSurfaceElevated,
    primarySoft = FlameSoftContainer,
    primarySoftBorder = FlameSoftBorder,
    amber = WarmAmber,
    amberContainer = WarmAmberContainer,
    amberBorder = WarmAmberBorder,
    flameStart = FlamePrimary,
    flameMid = FlameOrange,
    flameEnd = FlameGold,
    greenStart = WarmEmerald,
    greenEnd = WarmEmeraldBright,
    heroBgTop = WarmSurface,
    heroBgBottom = WarmBackgroundPeach,
    mealBlue = FlamePrimary,
    studyGreen = WarmEmerald,
    riceOrange = FlameOrange,
    neutralGrey = WarmNeutralGrey
)

private val DarkExtraColors = MessMateExtraColors(
    isDark = true,
    divider = DarkWarmDivider,
    borderStrong = DarkWarmOutlineStrong,
    success = DarkEmerald,
    successContainer = DarkEmeraldContainer,
    danger = DarkWarmDanger,
    dangerContainer = DarkWarmDangerContainer,
    warning = DarkWarmWarning,
    warningContainer = DarkWarmWarningContainer,
    surfaceLow = EspressoSurfaceLow,
    surfaceElevated = EspressoSurfaceElevated,
    primarySoft = DarkFlameSoftContainer,
    primarySoftBorder = DarkFlameSoftBorder,
    amber = DarkAmber,
    amberContainer = DarkAmberContainer,
    amberBorder = DarkAmberBorder,
    flameStart = DarkFlamePrimary,
    flameMid = DarkFlameOrange,
    flameEnd = DarkFlameGold,
    greenStart = DarkEmerald,
    greenEnd = DarkEmeraldBright,
    heroBgTop = EspressoSurface,
    heroBgBottom = EspressoBackgroundPeach,
    mealBlue = DarkFlamePrimary,
    studyGreen = DarkEmerald,
    riceOrange = DarkFlameOrange,
    neutralGrey = DarkWarmNeutralGrey
)

val LocalMessMateColors = staticCompositionLocalOf { LightExtraColors }

private val LightColorScheme = lightColorScheme(
    primary = FlamePrimary,
    onPrimary = Color.White,
    primaryContainer = FlameSoftContainer,
    onPrimaryContainer = FlamePrimary,
    secondary = WarmEmerald,
    onSecondary = Color.White,
    secondaryContainer = WarmEmeraldContainer,
    onSecondaryContainer = WarmEmeraldDarkText,
    tertiary = WarmAmber,
    onTertiary = Color.White,
    tertiaryContainer = WarmAmberContainer,
    onTertiaryContainer = WarmAmber,
    background = WarmBackground,
    onBackground = WarmPrimaryText,
    surface = WarmSurface,
    onSurface = WarmPrimaryText,
    surfaceVariant = WarmSurfaceElevated,
    onSurfaceVariant = WarmSecondaryText,
    surfaceContainerLowest = WarmSurface,
    surfaceContainerLow = WarmSurfaceLow,
    surfaceContainer = WarmSurfaceElevated,
    surfaceContainerHigh = WarmSurfaceContainer,
    outline = WarmOutlineStrong,
    outlineVariant = WarmDivider,
    error = WarmDanger,
    onError = Color.White,
    errorContainer = WarmDangerContainer,
    onErrorContainer = WarmDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkFlamePrimary,
    onPrimary = Color.White,
    primaryContainer = DarkFlameSoftContainer,
    onPrimaryContainer = DarkFlameGold,
    secondary = DarkEmerald,
    onSecondary = Color(0xFF06261B),
    secondaryContainer = DarkEmeraldContainer,
    onSecondaryContainer = DarkEmeraldBright,
    tertiary = DarkAmber,
    onTertiary = Color(0xFF281600),
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = DarkAmber,
    background = EspressoBackground,
    onBackground = DarkWarmPrimaryText,
    surface = EspressoSurface,
    onSurface = DarkWarmPrimaryText,
    surfaceVariant = EspressoSurfaceElevated,
    onSurfaceVariant = DarkWarmSecondaryText,
    surfaceContainerLowest = EspressoBackground,
    surfaceContainerLow = EspressoSurfaceLow,
    surfaceContainer = EspressoSurfaceElevated,
    surfaceContainerHigh = EspressoSurfaceContainer,
    outline = DarkWarmOutlineStrong,
    outlineVariant = DarkWarmDivider,
    error = DarkWarmDanger,
    onError = Color(0xFF2B0B0B),
    errorContainer = DarkWarmDangerContainer,
    onErrorContainer = DarkWarmDanger
)

@Composable
fun MessMateTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val extraColors = if (useDark) DarkExtraColors else LightExtraColors
    val reducedMotion = rememberSystemReducedMotion()

    CompositionLocalProvider(
        LocalMessMateColors provides extraColors,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MessMateTypography,
            content = content
        )
    }
}
