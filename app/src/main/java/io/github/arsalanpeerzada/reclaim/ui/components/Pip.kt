package io.github.arsalanpeerzada.reclaim.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
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
import kotlin.math.abs

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

/**
 * Pip, drawn on the mockup's 120 x 130 grid. Changes of look animate, so Pip visibly sags or perks up.
 * While [alive], Pip idles: a gentle bob, a blink every few seconds and a swaying antenna, all slower
 * and smaller the gloomier he is. Off when the user has removed animations.
 */
@Composable
fun Pip(look: PipLook, modifier: Modifier = Modifier, description: String? = null, alive: Boolean = true) {
    val joy by animateFloatAsState(look.joy, tween(600), label = "joy")
    val gloom by animateFloatAsState(look.gloom, tween(600), label = "gloom")
    val idle = alive && !rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "pip idle")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob",
    )
    val blink by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3800
                0f at 0
                0f at 3500
                1f at 3580
                0f at 3680
            },
        ),
        label = "blink",
    )
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    Canvas(if (description != null) modifier.semantics { contentDescription = description } else modifier) {
        val k = minOf(size.width / 120f, size.height / 130f)
        // Happy Pip bounces higher; gloomy Pip barely moves.
        val lift = if (idle) -bob * (3.5f - 2.5f * gloom) * k else 0f
        val squash = if (idle) 1f + bob * 0.02f * (1f - gloom) else 1f
        withTransform({
            translate((size.width - 120 * k) / 2, (size.height - 130 * k) / 2 + lift)
            scale(k, k * squash, pivot = Offset.Zero)
        }) {
            drawPip(
                joy = joy,
                gloom = gloom,
                eyesClosed = look.eyesClosed,
                openSmile = look.openSmile,
                blink = if (idle && !look.eyesClosed) blink else 0f,
                antennaSway = if (idle) sway * (6f - 4f * gloom) else 0f,
            )
        }
    }
}

private val Gloomy = Color(0xFFB4ADC4)
private val Tear = Color(0xFF57B8FF)

private fun DrawScope.drawPip(joy: Float, gloom: Float, eyesClosed: Boolean, openSmile: Boolean, blink: Float, antennaSway: Float) {
    val ink = Bright.Ink
    val outline = Stroke(width = 4f, cap = StrokeCap.Round)

    // Antenna: perky when bright, drooping to the side as gloom rises, swaying as Pip idles.
    val tip = Offset(76f + 6f * gloom + antennaSway, 7f + 22f * gloom + abs(antennaSway) * 0.3f)
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
        val open = 14f - 6f * gloom
        val eyeHeight = (open * (1f - blink)).coerceAtLeast(1.5f)
        val eyeTop = 61f + 6f * gloom + (open - eyeHeight) / 2
        drawOval(ink, topLeft = Offset(41f, eyeTop), size = Size(10f, eyeHeight))
        drawOval(ink, topLeft = Offset(69f, eyeTop), size = Size(10f, eyeHeight))
        if (gloom < 0.6f && blink < 0.5f) {
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
