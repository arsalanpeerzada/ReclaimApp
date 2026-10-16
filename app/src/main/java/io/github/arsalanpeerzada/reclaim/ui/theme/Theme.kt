package io.github.arsalanpeerzada.reclaim.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.arsalanpeerzada.reclaim.R

/**
 * The multicolour game palette from the approved mockups. The bright colours stay the same in dark
 * mode and always carry [ink] text; only the ground, cards and text swap.
 */
object Bright {
    val Violet = Color(0xFF6C4CF5)
    val Sunflower = Color(0xFFFFC83D)
    val Coral = Color(0xFFFF7657)
    val Sky = Color(0xFF57B8FF)
    val Mint = Color(0xFF34D1A0)
    val Pink = Color(0xFFFF94C6)
    val Ink = Color(0xFF1D1733)

    /** Cycled through for bars, ticks and progress segments. */
    val cycle = listOf(Violet, Sky, Mint, Sunflower, Coral, Pink)
}

@Immutable
data class ReclaimPalette(
    val ground: Color,
    val card: Color,
    val cardEdge: Color,
    val text: Color,
    val textMuted: Color,
    val track: Color,
    val link: Color,
    val softViolet: Color,
    val softSky: Color,
    val softMint: Color,
    val softCoral: Color,
    val softPink: Color,
    /** The garden's earth. */
    val soil: Color,
    /** Past-budget numbers and the meter's second lap, readable on this palette's cards. */
    val overflow: Color,
    /** The dark pill: main buttons and the navigation bar. */
    val pill: Color,
    val onPill: Color,
)

private val LightPalette = ReclaimPalette(
    ground = Color(0xFFFFF6EA),
    card = Color(0xFFFFFFFF),
    cardEdge = Color(0xFFEADFCF),
    text = Bright.Ink,
    textMuted = Color(0xFF5B5470),
    track = Color(0xFFEDE3D3),
    link = Color(0xFF4B2FD1),
    softViolet = Color(0xFFF3EEFF),
    softSky = Color(0xFFDDF0FF),
    softMint = Color(0xFFD6F7EC),
    softCoral = Color(0xFFFFE0D8),
    softPink = Color(0xFFFFE3F0),
    soil = Color(0xFFF1DEC6),
    overflow = Color(0xFF8E2F5A),
    pill = Bright.Ink,
    onPill = Color(0xFFFFFFFF),
)

private val DarkPalette = ReclaimPalette(
    ground = Color(0xFF15112A),
    card = Color(0xFF221C3A),
    cardEdge = Color(0xFF0E0B1D),
    text = Color(0xFFFFF6EA),
    textMuted = Color(0xFFC3BCD8),
    track = Color(0xFF332B52),
    link = Color(0xFFB9A8FF),
    softViolet = Color(0xFF2E2557),
    softSky = Color(0xFF1C3350),
    softMint = Color(0xFF173D36),
    softCoral = Color(0xFF45252A),
    softPink = Color(0xFF452440),
    soil = Color(0xFF3B2F52),
    overflow = Color(0xFFE0629B),
    pill = Color(0xFFFFF6EA),
    onPill = Bright.Ink,
)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

object ReclaimTheme {
    val palette: ReclaimPalette
        @Composable @ReadOnlyComposable
        get() = LocalPalette.current
}

@OptIn(ExperimentalTextApi::class)
private fun variable(resource: Int, weight: FontWeight) =
    Font(resource, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** Chunky display face for headings and big numbers. */
val Bricolage = FontFamily(
    variable(R.font.bricolage_grotesque, FontWeight.SemiBold),
    variable(R.font.bricolage_grotesque, FontWeight.ExtraBold),
)

/** Friendly body face. */
val DmSans = FontFamily(
    variable(R.font.dm_sans, FontWeight.Normal),
    variable(R.font.dm_sans, FontWeight.Medium),
    variable(R.font.dm_sans, FontWeight.Bold),
)

private fun display(size: Int, letterSpacing: Float = 0f) = TextStyle(
    fontFamily = Bricolage,
    fontWeight = FontWeight.ExtraBold,
    fontSize = size.sp,
    lineHeight = (size * 1.05f).sp,
    letterSpacing = letterSpacing.sp,
)

private fun body(size: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = DmSans, fontWeight = weight, fontSize = size.sp, lineHeight = (size * 1.45f).sp)

private val ReclaimTypography = Typography(
    displayLarge = display(72, -2f),
    displayMedium = display(58, -1.5f),
    displaySmall = display(40, -1f),
    headlineLarge = display(34, -0.8f),
    headlineMedium = display(28, -0.5f),
    headlineSmall = display(24),
    titleLarge = display(18),
    titleMedium = body(16, FontWeight.Bold),
    titleSmall = body(15, FontWeight.Bold),
    bodyLarge = body(16),
    bodyMedium = body(14),
    bodySmall = body(12),
    labelLarge = body(16, FontWeight.Bold),
    labelMedium = body(13, FontWeight.Bold),
    labelSmall = body(12, FontWeight.Bold),
)

@Composable
fun ReclaimTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) DarkPalette else LightPalette
    // Material components (sliders, progress) pick up the same colours.
    val scheme = if (dark) {
        darkColorScheme(
            primary = Bright.Violet,
            onPrimary = Color.White,
            secondary = Bright.Sky,
            tertiary = Bright.Coral,
            background = palette.ground,
            surface = palette.ground,
            onBackground = palette.text,
            onSurface = palette.text,
            onSurfaceVariant = palette.textMuted,
        )
    } else {
        lightColorScheme(
            primary = Bright.Violet,
            onPrimary = Color.White,
            secondary = Bright.Sky,
            tertiary = Bright.Coral,
            background = palette.ground,
            surface = palette.ground,
            onBackground = palette.text,
            onSurface = palette.text,
            onSurfaceVariant = palette.textMuted,
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = ReclaimTypography, content = content)
    }
}
