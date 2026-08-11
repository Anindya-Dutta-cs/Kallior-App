package kallos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Core palette ──────────────────────────────────────────────
val KalliorBackground     = Color(0xFF0F0F0F)
val KalliorSurface        = Color(0xFF1A1A1A)
val KalliorSurfaceVariant = Color(0xFF242424)
val KalliorMuted          = Color(0xFF8A8A8A)
val KalliorOnSurface      = Color(0xFFE8E8E8)
val KalliorOnBackground   = Color(0xFFFFFFFF)

val KalliorPrimary         = Color(0xFF7C5CFC) // violet accent
val KalliorPrimaryVariant  = Color(0xFF9B7FFF)
val KalliorOnPrimary       = Color(0xFFFFFFFF)
val KalliorPrimaryContainer    = Color(0xFF2A2040)
val KalliorOnPrimaryContainer  = Color(0xFFD4C4FF)

val KalliorSecondary          = Color(0xFF4ECDC4) // teal accent
val KalliorSecondaryContainer = Color(0xFF1A3533)
val KalliorOnSecondaryContainer = Color(0xFFA8EDE8)

val KalliorTertiary          = Color(0xFFFF8A65) // warm accent
val KalliorTertiaryContainer = Color(0xFF3D2418)
val KalliorOnTertiaryContainer = Color(0xFFFFCCB5)

val KalliorError          = Color(0xFFFF5252)
val KalliorErrorContainer = Color(0xFF3D1414)
val KalliorOnError        = Color(0xFFFFFFFF)
val KalliorOnErrorContainer = Color(0xFFFFB4AB)

val KalliorOutline        = Color(0xFF333333)

private val KalliorDarkColorScheme = darkColorScheme(
    primary = KalliorPrimary,
    onPrimary = KalliorOnPrimary,
    primaryContainer = KalliorPrimaryContainer,
    onPrimaryContainer = KalliorOnPrimaryContainer,
    secondary = KalliorSecondary,
    secondaryContainer = KalliorSecondaryContainer,
    onSecondaryContainer = KalliorOnSecondaryContainer,
    tertiary = KalliorTertiary,
    tertiaryContainer = KalliorTertiaryContainer,
    onTertiaryContainer = KalliorOnTertiaryContainer,
    error = KalliorError,
    errorContainer = KalliorErrorContainer,
    onError = KalliorOnError,
    onErrorContainer = KalliorOnErrorContainer,
    background = KalliorBackground,
    onBackground = KalliorOnBackground,
    surface = KalliorSurface,
    onSurface = KalliorOnSurface,
    surfaceVariant = KalliorSurfaceVariant,
    onSurfaceVariant = KalliorMuted,
    outline = KalliorOutline,
)

// ── Typography ────────────────────────────────────────────────
private val KalliorTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp,
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ── Theme composable ─────────────────────────────────────────
@Composable
fun KalliorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KalliorDarkColorScheme,
        typography = KalliorTypography,
        content = content,
    )
}
