package io.github.arsalanpeerzada.reclaim.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Whether the user has turned off animations in Android's settings (Remove animations sets the
 * animator duration scale to 0). Idle loops and confetti respect it; one-off transitions jump to the end.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Fades and rises into place the first time it's shown, [index] steps after the first item. */
fun Modifier.appearIn(index: Int = 0): Modifier = composed {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 420, delayMillis = index * 70, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

/** Wraps a full-width block so it fades and rises in, [index] steps after the first. */
@Composable
fun Appear(index: Int, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().appearIn(index)) { content() }
}

/** Pops in with a little overshoot, like a game tile landing. */
fun Modifier.popIn(index: Int = 0): Modifier = composed {
    val reduced = rememberReducedMotion()
    val scale = remember { Animatable(if (reduced) 1f else 0.6f) }
    val alpha = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index.coerceAtMost(12) * 45L)
        alpha.animateTo(1f, tween(160))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index.coerceAtMost(12) * 45L)
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    graphicsLayer {
        this.alpha = alpha.value
        scaleX = scale.value
        scaleY = scale.value
    }
}

/** A number that rolls up when it grows and down when it shrinks. */
@Composable
fun RollingNumber(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            val up = (targetState.filter(Char::isDigit).toIntOrNull() ?: 0) >= (initialState.filter(Char::isDigit).toIntOrNull() ?: 0)
            (slideInVertically(tween(260)) { if (up) it else -it } + fadeIn(tween(260)))
                .togetherWith(slideOutVertically(tween(200)) { if (up) -it else it } + fadeOut(tween(200)))
        },
        label = "rolling number",
        modifier = modifier,
    ) { shown ->
        Text(shown, style = style, color = color)
    }
}

private class Particle(val angle: Float, val speed: Float, val spin: Float, val size: Float, val colour: Color, val round: Boolean)

/**
 * A one-shot confetti burst from the centre, played each time [trigger] changes to a new non-null
 * value. Draws nothing at rest, so it can sit over any content.
 */
@Composable
fun ConfettiBurst(trigger: Any?, modifier: Modifier = Modifier, pieces: Int = 36, colours: List<Color> = Bright.cycle) {
    val reduced = rememberReducedMotion()
    val progress = remember { Animatable(1f) }
    val particles = remember(trigger) {
        val random = Random(trigger.hashCode())
        List(pieces) {
            Particle(
                angle = random.nextFloat() * 360f,
                speed = 0.45f + random.nextFloat() * 0.55f,
                spin = random.nextFloat() * 720f - 360f,
                size = 5f + random.nextFloat() * 6f,
                colour = colours[it % colours.size],
                round = random.nextBoolean(),
            )
        }
    }
    LaunchedEffect(trigger) {
        if (trigger == null || reduced) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1300, easing = LinearEasing))
    }
    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val reach = size.minDimension * 0.6f
        val centre = Offset(size.width / 2, size.height / 2)
        particles.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            // Out fast, then gravity takes over.
            val travel = reach * p.speed * (1f - (1f - t) * (1f - t))
            val x = centre.x + cos(rad).toFloat() * travel
            val y = centre.y + sin(rad).toFloat() * travel + 0.9f * reach * t * t
            val alpha = (1f - t).coerceIn(0f, 1f)
            val s = p.size.dp.toPx()
            rotate(p.spin * t, pivot = Offset(x, y)) {
                if (p.round) {
                    drawCircle(p.colour.copy(alpha = alpha), radius = s / 2, center = Offset(x, y))
                } else {
                    drawRect(p.colour.copy(alpha = alpha), topLeft = Offset(x - s / 2, y - s / 4), size = Size(s, s / 2))
                }
            }
        }
    }
}
