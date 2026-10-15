package io.github.arsalanpeerzada.reclaim.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.arsalanpeerzada.reclaim.domain.Progress
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright

/** Fixed moods for places that aren't about today's budget. */
enum class PipMood { Happy, Focused, Calm, Proud }

/**
 * How Pip looks. [joy] runs from 1 (big smile) to -1 (frown); [gloom] from 0 (bright yellow, perky)
 * to 1 (grey, drooping, in tears). Pip only ever shows his own feelings: the app never scolds.
 */
data class PipLook(
    val joy: Float,
    val gloom: Float,
    val eyesClosed: Boolean = false,
    val openSmile: Boolean = false,
) {
    companion object {
        /** Pip over today's budget: from wary near it to heartbroken at 250%. Fully recovered tomorrow. */
        fun forBudget(fraction: Float): PipLook = when {
            fraction < 0.5f -> PipLook(joy = 1f, gloom = 0f)
            fraction < Progress.NUDGE_FRACTION -> PipLook(joy = 0.7f, gloom = 0f)
            fraction <= 1f -> PipLook(joy = 0.05f, gloom = 0.05f)
            else -> {
                val over = ((fraction - 1f) / 1.5f).coerceIn(0f, 1f)
                PipLook(joy = -0.35f - 0.65f * over, gloom = 0.2f + 0.8f * over)
            }
        }

        fun of(mood: PipMood): PipLook = when (mood) {
            PipMood.Happy -> PipLook(joy = 1f, gloom = 0f)
            PipMood.Focused -> PipLook(joy = 0.05f, gloom = 0.05f)
            PipMood.Calm -> PipLook(joy = 0.6f, gloom = 0f, eyesClosed = true)
            PipMood.Proud -> PipLook(joy = 1f, gloom = 0f, openSmile = true)
        }
    }
}

@Composable
fun Pip(mood: PipMood, modifier: Modifier = Modifier, description: String? = null) =
    Pip(PipLook.of(mood), modifier, description)

/** Pip, drawn on the mockup's 120 x 130 grid. Changes of look animate, so Pip visibly sags or perks up. */
@Composable
fun Pip(look: PipLook, modifier: Modifier = Modifier, description: String? = null) {
    val joy by animateFloatAsState(look.joy, tween(600), label = "joy")
    val gloom by animateFloatAsState(look.gloom, tween(600), label = "gloom")
    Canvas(if (description != null) modifier.semantics { contentDescription = description } else modifier) {
        val k = minOf(size.width / 120f, size.height / 130f)
        withTransform({
            translate((size.width - 120 * k) / 2, (size.height - 130 * k) / 2)
            scale(k, k, pivot = Offset.Zero)
        }) {
            drawPip(joy, gloom, look.eyesClosed, look.openSmile)
        }
    }
}

private val Gloomy = Color(0xFFB4ADC4)
private val Tear = Color(0xFF57B8FF)

private fun DrawScope.drawPip(joy: Float, gloom: Float, eyesClosed: Boolean, openSmile: Boolean) {
    val ink = Bright.Ink
    val outline = Stroke(width = 4f, cap = StrokeCap.Round)

    // Antenna: perky when bright, drooping to the side as gloom rises.
    val tip = Offset(76f + 6f * gloom, 7f + 22f * gloom)
    val antenna = Path().apply {
        moveTo(60f, 22f)
        cubicTo(60f, 14f - 4f * gloom, 65f + 4f * gloom, 9f + 4f * gloom, tip.x - 3f, tip.y)
    }
    drawPath(antenna, ink, style = outline)
    drawCircle(lerp(Bright.Coral, Gloomy, gloom), radius = 6f, center = tip)
    drawCircle(ink, radius = 6f, center = tip, style = Stroke(3f))

    // Body: sunflower yellow fading to grey.
    val body = Path().apply {
        moveTo(60f, 22f)
        cubicTo(86f, 22f, 106f, 42f, 106f, 70f)
        cubicTo(106f, 98f, 88f, 118f, 60f, 118f)
        cubicTo(32f, 118f, 14f, 98f, 14f, 70f)
        cubicTo(14f, 42f, 34f, 22f, 60f, 22f)
        close()
    }
    drawPath(body, lerp(Bright.Sunflower, Gloomy, gloom))
    drawPath(body, ink, style = outline)

    // Cheeks fade as Pip loses colour.
    val cheek = Bright.Pink.copy(alpha = (1f - gloom * 1.4f).coerceIn(0f, 1f))
    drawOval(cheek, topLeft = Offset(29f, 78f), size = Size(14f, 8f))
    drawOval(cheek, topLeft = Offset(77f, 78f), size = Size(14f, 8f))

    // Eyes: heavier lids as gloom rises.
    if (eyesClosed) {
        val lid = Stroke(width = 4f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(40f, 69f); quadraticTo(46f, 64f, 52f, 69f) }, ink, style = lid)
        drawPath(Path().apply { moveTo(68f, 69f); quadraticTo(74f, 64f, 80f, 69f) }, ink, style = lid)
    } else {
        val eyeHeight = 14f - 6f * gloom
        val eyeTop = 61f + 6f * gloom
        drawOval(ink, topLeft = Offset(41f, eyeTop), size = Size(10f, eyeHeight))
        drawOval(ink, topLeft = Offset(69f, eyeTop), size = Size(10f, eyeHeight))
        if (gloom < 0.6f) {
            drawCircle(Color.White, radius = 2f, center = Offset(48f, eyeTop + 3f))
            drawCircle(Color.White, radius = 2f, center = Offset(76f, eyeTop + 3f))
        }
    }

    // Worried brows once Pip is unhappy: inner ends lift as joy falls.
    val worry = (-joy).coerceIn(0f, 1f)
    if (worry > 0f) {
        val brow = Stroke(width = 3.5f, cap = StrokeCap.Round)
        val lift = 2f + 6f * worry
        drawLine(ink, Offset(37f, 56f), Offset(51f, 56f - lift), strokeWidth = brow.width, cap = StrokeCap.Round)
        drawLine(ink, Offset(83f, 56f), Offset(69f, 56f - lift), strokeWidth = brow.width, cap = StrokeCap.Round)
    }

    // Mouth: smile to frown.
    if (openSmile) {
        drawPath(Path().apply { moveTo(46f, 84f); cubicTo(50f, 96f, 70f, 96f, 74f, 84f); close() }, ink)
    } else {
        val mouthY = 87f + 2f * worry
        val mouth = Path().apply {
            moveTo(48f, mouthY)
            quadraticTo(60f, mouthY + 12f * joy, 72f, mouthY)
        }
        drawPath(mouth, ink, style = outline)
    }

    // A nervous sweat drop when just over, tears as it gets worse.
    if (gloom in 0.15f..0.5f) drawDrop(Offset(95f, 46f), 1f - (gloom - 0.15f) / 0.35f * 0.5f)
    if (gloom > 0.45f) drawDrop(Offset(45f, 82f + 6f * gloom), ((gloom - 0.45f) / 0.2f).coerceIn(0f, 1f))
    if (gloom > 0.75f) drawDrop(Offset(75f, 84f + 6f * gloom), ((gloom - 0.75f) / 0.2f).coerceIn(0f, 1f))
}

private fun DrawScope.drawDrop(at: Offset, alpha: Float) {
    if (alpha <= 0f) return
    val drop = Path().apply {
        moveTo(at.x, at.y - 7f)
        cubicTo(at.x + 5f, at.y, at.x + 5f, at.y + 5f, at.x, at.y + 5f)
        cubicTo(at.x - 5f, at.y + 5f, at.x - 5f, at.y, at.x, at.y - 7f)
        close()
    }
    drawPath(drop, Tear.copy(alpha = alpha))
}
