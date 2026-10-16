package io.github.arsalanpeerzada.reclaim.ui.components

import androidx.compose.ui.graphics.Color
import io.github.arsalanpeerzada.reclaim.domain.Progress
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright

/**
 * The budget meter shared by the Today ring and the live pill: ten segments, a tenth of the budget
 * each, warming from violet to coral. Past the budget a second lap fills in dark plum, so the meter
 * keeps tracking every reel instead of stopping when it's full.
 */
object BudgetMeter {
    const val SEGMENTS = 10

    val colours = listOf(
        Bright.Violet, Bright.Violet, Bright.Sky, Bright.Sky, Bright.Mint,
        Bright.Mint, Bright.Sunflower, Bright.Sunflower, Bright.Coral, Bright.Coral,
    )

    /** The second lap, past the budget, on light backgrounds. Darker than every lap-one colour. */
    val Overflow = Color(0xFF8E2F5A)

    /** The same plum, lifted so it stays readable on dark backgrounds (the pill, dark mode). */
    val OverflowOnDark = Color(0xFFE0629B)

    /** How full segment [i] is (0 to 1) in the first lap; partly filled segments move with every reel. */
    fun firstLap(fraction: Float, i: Int): Float = (fraction * SEGMENTS - i).coerceIn(0f, 1f)

    /** How full segment [i] is in the overflow lap: 100% to 200% of the budget. */
    fun secondLap(fraction: Float, i: Int): Float = ((fraction - 1f) * SEGMENTS - i).coerceIn(0f, 1f)
}

/** What Pip says about today. Always Pip's own feelings, never a judgement of the user. */
data class PipLine(val headline: String, val detail: String)

fun pipLine(reels: Int, budget: Int): PipLine {
    val left = budget - reels
    val fraction = if (budget > 0) reels.toFloat() / budget else 1f
    val over = -left
    return when {
        fraction > 2f -> PipLine("Pip's heartbroken.", "$over over today's budget. He'll bounce back tomorrow, promise.")
        fraction > 1.5f -> PipLine("Pip's really down.", "$over over today. A quest might cheer you both up.")
        fraction > 1.25f -> PipLine("Pip's feeling it.", "$over over today's budget. Maybe call it a day?")
        reels > budget -> PipLine("Pip's a bit worried.", "$over over today's budget. Still choosing this?")
        left == 0 -> PipLine("Right on budget.", "Any more and Pip starts to worry.")
        fraction >= Progress.NUDGE_FRACTION -> PipLine("Getting close.", "Still choosing this? $left left today.")
        fraction >= 0.5f -> PipLine("On track.", "$left reels left in today's budget.")
        else -> PipLine("Plenty of room.", "$left reels left in today's budget.")
    }
}
