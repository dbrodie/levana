package ninja.doskey.app.levana.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ninja.doskey.app.levana.R

// Bundled fonts keep the reference typography available without a network connection.
@OptIn(ExperimentalTextApi::class)
val LevanaDisplayFontFamily = FontFamily(
    Font(R.font.frank_ruhl_libre, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.frank_ruhl_libre, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.frank_ruhl_libre, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.frank_ruhl_libre, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)
@OptIn(ExperimentalTextApi::class)
val LevanaBodyFontFamily = FontFamily(
    Font(R.font.manrope, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.manrope, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.manrope, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

private fun display(size: Int, line: Int) = TextStyle(
    fontFamily = LevanaDisplayFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = size.sp,
    lineHeight = line.sp,
)
private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = LevanaBodyFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
)

val LevanaTypography = Typography(
    displayLarge = display(57, 64), displayMedium = display(45, 52), displaySmall = display(36, 44),
    headlineLarge = display(32, 40), headlineMedium = display(28, 36), headlineSmall = display(24, 32),
    titleLarge = display(22, 28), titleMedium = body(16, 24, FontWeight.SemiBold),
    titleSmall = body(14, 20, FontWeight.SemiBold),
    bodyLarge = body(16, 24), bodyMedium = body(14, 20), bodySmall = body(12, 16),
    labelLarge = body(14, 20, FontWeight.SemiBold),
    labelMedium = body(12, 16, FontWeight.SemiBold),
    labelSmall = body(11, 16, FontWeight.SemiBold),
)
