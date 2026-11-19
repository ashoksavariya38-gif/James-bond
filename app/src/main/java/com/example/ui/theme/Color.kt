package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Sparkle-Gradient Accent Palette (#4285F4 -> #8B5CF6 -> #06B6D4)
val BrandBlue = Color(0xFF4285F4)
val BrandPurple = Color(0xFF8B5CF6)
val BrandCyan = Color(0xFF06B6D4)

val SparkleGradientStops: List<Color> = listOf(BrandBlue, BrandPurple, BrandCyan)

// Dark Mode Tokens (Default - Deep Slate #0F1117)
val DarkBackground = Color(0xFF0F1117)
val DarkSidebar = Color(0xFF171923)
val DarkElevatedCard = Color(0xFF1E2130)
val DarkSurfaceVariant = Color(0xFF232738)
val DarkBorderSubtle = Color(0xFF2A2F42)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkCodeSurface = Color(0xFF121521)
val DarkMathSurface = Color(0xFF181D2E)
val DarkPrimaryContainer = Color(0xFF1F2A48)
val DarkOnPrimaryContainer = Color(0xFFD8E4FF)
val DarkSecondaryContainer = Color(0xFF2C234B)
val DarkOnSecondaryContainer = Color(0xFFE9DDFF)
val DarkOnTertiary = Color(0xFF042F38)

// Light Mode Tokens
val LightBackground = Color(0xFFF8FAFC)
val LightSidebar = Color(0xFFF1F5F9)
val LightElevatedCard = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8EEF5)
val LightBorderSubtle = Color(0xFFE2E8F0)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightCodeSurface = Color(0xFFF1F5F9)
val LightMathSurface = Color(0xFFEEF2FF)
val LightPrimaryContainer = Color(0xFFE0ECFF)
val LightOnPrimaryContainer = Color(0xFF0A2E66)
val LightSecondaryContainer = Color(0xFFEDE9FE)
val LightOnSecondaryContainer = Color(0xFF3B1A78)

// Semantic Status & Identity Tokens
val BrandSuccess = Color(0xFF10B981)
val BrandWarning = Color(0xFFF59E0B)
val BrandError = Color(0xFFEF4444)

val GoogleBrandRed = Color(0xFFEA4335)
val GoogleBrandYellow = Color(0xFFFBBC05)
val GoogleBrandGreen = Color(0xFF34A853)
val GoogleBrandBlue = Color(0xFF4285F4)

// Shared Sparkle-Gradient Brushes
val LuminaBrandGradient = Brush.linearGradient(colors = SparkleGradientStops)
val LuminaHorizontalGradient = Brush.horizontalGradient(colors = SparkleGradientStops)
