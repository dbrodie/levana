package ninja.doskey.app.levana.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Neutral surfaces stay constant when a holiday changes the accent colors.
internal val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3B4A55), onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E1E8), onPrimaryContainer = Color(0xFF16242C),
    inversePrimary = Color(0xFFA8C7DA),
    secondary = Color(0xFF5A6570), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E5E9), onSecondaryContainer = Color(0xFF1A2226),
    tertiary = Color(0xFF7A5B3A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5E0CA), onTertiaryContainer = Color(0xFF2A1806),
    background = Color(0xFFFAF9F7), onBackground = Color(0xFF1B1C1E),
    surface = Color(0xFFFAF9F7), onSurface = Color(0xFF1B1C1E),
    surfaceVariant = Color(0xFFE5E1DC), onSurfaceVariant = Color(0xFF45484C),
    surfaceTint = Color(0xFF3B4A55),
    surfaceBright = Color(0xFFFAF9F7), surfaceDim = Color(0xFFDAD6D1),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF3F1ED),
    surfaceContainer = Color(0xFFECE9E5), surfaceContainerHigh = Color(0xFFE5E1DC),
    surfaceContainerHighest = Color(0xFFDEDAD5),
    outline = Color(0xFF767A7E), outlineVariant = Color(0xFFDAD6D1),
    inverseSurface = Color(0xFF2A2C2F), inverseOnSurface = Color(0xFFF3F1ED),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    scrim = Color(0xFF1B1C1E),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7DA), onPrimary = Color(0xFF16242C),
    primaryContainer = Color(0xFF33424B), onPrimaryContainer = Color(0xFFD6E1E8),
    inversePrimary = Color(0xFF3B4A55),
    secondary = Color(0xFFBAC7D0), onSecondary = Color(0xFF243039),
    secondaryContainer = Color(0xFF3C464D), onSecondaryContainer = Color(0xFFE0E5E9),
    tertiary = Color(0xFFD4A96C), onTertiary = Color(0xFF2A1806),
    tertiaryContainer = Color(0xFF463525), onTertiaryContainer = Color(0xFFF5E0CA),
    background = Color(0xFF131517), onBackground = Color(0xFFDAD6D1),
    surface = Color(0xFF131517), onSurface = Color(0xFFDAD6D1),
    surfaceVariant = Color(0xFF2A2C2F), onSurfaceVariant = Color(0xFFC8C7C4),
    surfaceTint = Color(0xFFA8C7DA),
    surfaceBright = Color(0xFF383A3D), surfaceDim = Color(0xFF131517),
    surfaceContainerLowest = Color(0xFF0E1012), surfaceContainerLow = Color(0xFF1B1D1F),
    surfaceContainer = Color(0xFF1F2123), surfaceContainerHigh = Color(0xFF2A2C2F),
    surfaceContainerHighest = Color(0xFF35373A),
    outline = Color(0xFF919599), outlineVariant = Color(0xFF4C5054),
    inverseSurface = Color(0xFFDAD6D1), inverseOnSurface = Color(0xFF1B1C1E),
    error = Color(0xFFFFB4AB), onError = Color(0xFF410E0B),
    errorContainer = Color(0xFF5A2A26), onErrorContainer = Color(0xFFFFB4AB),
    scrim = Color.Black,
)

/**
 * The app's color scheme: the holiday accents when [holidayTheme] is set, otherwise
 * [dynamicScheme] (wallpaper colors) when given, otherwise Levana's own colors. Shared by
 * [LevanaTheme] and the home-screen widgets so both always color alike.
 */
fun levanaColorScheme(
    darkTheme: Boolean,
    holidayTheme: HolidayTheme?,
    dynamicScheme: ColorScheme? = null,
): ColorScheme = when {
    holidayTheme != null -> if (darkTheme) holidayTheme.darkScheme() else holidayTheme.lightScheme()
    dynamicScheme != null -> dynamicScheme
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}

/** Whether the device offers Material You wallpaper colors (Android 12+; always, at minSdk 34). */
val supportsMaterialYou: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** The wallpaper's Material You colors. */
fun wallpaperColorScheme(context: Context, darkTheme: Boolean): ColorScheme =
    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

/**
 * [levanaColorScheme] with the wallpaper's colors standing in for Levana's when [materialYou]
 * is on and supported. A holiday theme still wins over them.
 */
fun levanaColorScheme(
    context: Context,
    darkTheme: Boolean,
    holidayTheme: HolidayTheme?,
    materialYou: Boolean,
): ColorScheme = levanaColorScheme(
    darkTheme = darkTheme,
    holidayTheme = holidayTheme,
    dynamicScheme = if (materialYou && supportsMaterialYou) wallpaperColorScheme(context, darkTheme) else null,
)

@Composable
fun LevanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    materialYou: Boolean = false,
    holidayTheme: HolidayTheme? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = levanaColorScheme(LocalContext.current, darkTheme, holidayTheme, materialYou)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LevanaTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
