package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class LuminaExtendedColors(
  val deepSlateBackground: Color,
  val sidebar: Color,
  val elevatedCard: Color,
  val borderSubtle: Color,
  val textSecondary: Color,
  val codeSurface: Color,
  val mathSurface: Color,
  val brandBlue: Color,
  val brandPurple: Color,
  val brandCyan: Color,
  val sparkleGradientStops: List<Color>,
  val brandGradient: Brush,
  val success: Color,
  val warning: Color,
  val darkModeActive: Boolean,
)

private val DarkExtendedColors = LuminaExtendedColors(
  deepSlateBackground = DarkBackground,
  sidebar = DarkSidebar,
  elevatedCard = DarkElevatedCard,
  borderSubtle = DarkBorderSubtle,
  textSecondary = DarkTextSecondary,
  codeSurface = DarkCodeSurface,
  mathSurface = DarkMathSurface,
  brandBlue = BrandBlue,
  brandPurple = BrandPurple,
  brandCyan = BrandCyan,
  sparkleGradientStops = SparkleGradientStops,
  brandGradient = LuminaBrandGradient,
  success = BrandSuccess,
  warning = BrandWarning,
  darkModeActive = true,
)

private val LightExtendedColors = LuminaExtendedColors(
  deepSlateBackground = DarkBackground,
  sidebar = LightSidebar,
  elevatedCard = LightElevatedCard,
  borderSubtle = LightBorderSubtle,
  textSecondary = LightTextSecondary,
  codeSurface = LightCodeSurface,
  mathSurface = LightMathSurface,
  brandBlue = BrandBlue,
  brandPurple = BrandPurple,
  brandCyan = BrandCyan,
  sparkleGradientStops = SparkleGradientStops,
  brandGradient = LuminaBrandGradient,
  success = BrandSuccess,
  warning = BrandWarning,
  darkModeActive = false,
)

val LocalLuminaColors = staticCompositionLocalOf { DarkExtendedColors }

object LuminaTheme {
  val colors: LuminaExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLuminaColors.current
}

private val LuminaDarkColorScheme = darkColorScheme(
  primary = BrandBlue,
  onPrimary = Color.White,
  primaryContainer = DarkPrimaryContainer,
  onPrimaryContainer = DarkOnPrimaryContainer,
  secondary = BrandPurple,
  onSecondary = Color.White,
  secondaryContainer = DarkSecondaryContainer,
  onSecondaryContainer = DarkOnSecondaryContainer,
  tertiary = BrandCyan,
  onTertiary = DarkOnTertiary,
  background = DarkBackground,
  onBackground = DarkTextPrimary,
  surface = DarkElevatedCard,
  onSurface = DarkTextPrimary,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkBorderSubtle,
  outlineVariant = DarkBorderSubtle,
  error = BrandError,
  onError = Color.White,
)

private val LuminaLightColorScheme = lightColorScheme(
  primary = BrandBlue,
  onPrimary = Color.White,
  primaryContainer = LightPrimaryContainer,
  onPrimaryContainer = LightOnPrimaryContainer,
  secondary = BrandPurple,
  onSecondary = Color.White,
  secondaryContainer = LightSecondaryContainer,
  onSecondaryContainer = LightOnSecondaryContainer,
  tertiary = BrandCyan,
  onTertiary = Color.White,
  background = LightBackground,
  onBackground = LightTextPrimary,
  surface = LightElevatedCard,
  onSurface = LightTextPrimary,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = LightTextSecondary,
  outline = LightBorderSubtle,
  outlineVariant = LightBorderSubtle,
  error = BrandError,
  onError = Color.White,
)

val LuminaShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(14.dp),
  large = RoundedCornerShape(20.dp),
  extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun LuminaStudyTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) LuminaDarkColorScheme else LuminaLightColorScheme
  val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

  CompositionLocalProvider(LocalLuminaColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = LuminaTypography,
      shapes = LuminaShapes,
      content = content,
    )
  }
}
