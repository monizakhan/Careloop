package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CareLoopLightColorScheme = lightColorScheme(
  primary = SidebarDarkGreen,
  onPrimary = Color.White,
  primaryContainer = SoftGreenSurface,
  onPrimaryContainer = HeadingDarkGreen,
  secondary = SuccessGreenText,
  onSecondary = Color.White,
  secondaryContainer = SuccessGreenBg,
  onSecondaryContainer = SuccessGreenText,
  tertiary = RiskMediumText,
  onTertiary = Color.White,
  tertiaryContainer = RiskMediumBg,
  onTertiaryContainer = RiskMediumText,
  background = PageBackground,
  onBackground = BodyInkPrimary,
  surface = CardSurface,
  onSurface = BodyInkPrimary,
  surfaceVariant = TableHeaderSurface,
  onSurfaceVariant = BodyInkSecondary,
  outline = HairlineBorder,
  outlineVariant = HairlineBorder,
  error = RiskHighText,
  onError = Color.White,
  errorContainer = RiskHighBg,
  onErrorContainer = RiskHighText,
)

@Composable
fun CareLoopTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = CareLoopLightColorScheme,
    typography = CareLoopTypography,
    content = content,
  )
}
