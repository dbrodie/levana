package ninja.doskey.app.levana.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The platform font pairs Latin with a matching-weight Hebrew font and follows the device's font choice.
val LevanaDisplayFontFamily = FontFamily.Default
val LevanaBodyFontFamily = FontFamily.Default

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
