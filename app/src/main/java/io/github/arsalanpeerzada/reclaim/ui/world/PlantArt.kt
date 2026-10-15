package io.github.arsalanpeerzada.reclaim.ui.world

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import io.github.arsalanpeerzada.reclaim.domain.PlantKind
import io.github.arsalanpeerzada.reclaim.domain.Seedling
import io.github.arsalanpeerzada.reclaim.ui.components.rememberReducedMotion
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Stem = Color(0xFF22A57C)
private val Bark = Color(0xFF7A4A2E)
private val Wilt = Color(0xFFA79F95)

/**
 * One plant, drawn on a 100 x 100 grid with its base at (50, 88). A wilted plant leans and loses
 * some colour: drooping, never dead.
 */
@Composable
fun PlantArt(kind: PlantKind, wilted: Boolean, modifier: Modifier = Modifier) {
    // A light breeze: each kind sways at its own pace so the garden never moves in lockstep.
    val still = rememberReducedMotion()
    val breeze by rememberInfiniteTransition(label = "breeze").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2100 + kind.ordinal * 230, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    Canvas(modifier) {
        val u = size.minDimension / 100f
        val tint: (Color) -> Color = { if (wilted) lerp(it, Wilt, 0.55f) else it }
        val sway = if (still) 0f else breeze * (if (wilted) 1f else 3f)
        withTransform({
            translate((size.width - 100 * u) / 2, (size.height - 100 * u) / 2)
            rotate((if (wilted) 16f else 0f) + sway, pivot = Offset(50 * u, 88 * u))
        }) {
            drawPlant(kind, u, tint)
        }
    }
}

/** Today's plot: a sprout while within budget, a resting seed when over. */
@Composable
fun SeedlingArt(state: Seedling, modifier: Modifier = Modifier) {
    // The sprout breathes upwards, as if it's growing while you watch.
    val still = rememberReducedMotion()
    val grow by rememberInfiniteTransition(label = "sprout").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "grow",
    )
    Canvas(modifier) {
        val u = size.minDimension / 100f
        val g = if (still || state != Seedling.Growing) 1f else grow
        withTransform({
            translate((size.width - 100 * u) / 2, (size.height - 100 * u) / 2)
            scale(1f, g, pivot = Offset(50 * u, 88 * u))
        }) {
            when (state) {
                Seedling.Growing -> {
                    stem(u, 66f, Stem)
                    leaf(u, Offset(50f, 70f), -35f, Stem)
                    leaf(u, Offset(50f, 66f), 215f, Stem)
                }
                Seedling.Resting -> {
                    drawOval(Bark, topLeft = Offset(42 * u, 78 * u), size = Size(16 * u, 10 * u))
                    drawCircle(Bright.Sky, radius = 3 * u, center = Offset(64 * u, 62 * u))
                    drawCircle(Bright.Sky, radius = 2 * u, center = Offset(70 * u, 54 * u))
                }
            }
        }
    }
}

private fun DrawScope.drawPlant(kind: PlantKind, u: Float, tint: (Color) -> Color) {
    val ink = tint(Bright.Ink)
    val outline = Stroke(width = 2.2f * u)
    fun p(x: Float, y: Float) = Offset(x * u, y * u)

    when (kind) {
        PlantKind.Tulip -> {
            stem(u, 46f, tint(Stem))
            leaf(u, Offset(50f, 74f), -30f, tint(Stem))
            val cup = Path().apply {
                moveTo(38 * u, 46 * u)
                quadraticTo(37 * u, 28 * u, 43 * u, 24 * u)
                lineTo(47 * u, 33 * u)
                lineTo(50 * u, 22 * u)
                lineTo(53 * u, 33 * u)
                lineTo(57 * u, 24 * u)
                quadraticTo(63 * u, 28 * u, 62 * u, 46 * u)
                quadraticTo(50 * u, 54 * u, 38 * u, 46 * u)
                close()
            }
            drawPath(cup, tint(Bright.Pink))
            drawPath(cup, ink, style = outline)
        }
        PlantKind.Daisy -> {
            stem(u, 44f, tint(Stem))
            petals(u, Offset(50f, 38f), count = 8, distance = 11f, radius = 7f, colours = listOf(tint(Bright.Sky)), ink = ink)
            drawCircle(tint(Bright.Sunflower), radius = 6 * u, center = p(50f, 38f))
            drawCircle(ink, radius = 6 * u, center = p(50f, 38f), style = outline)
        }
        PlantKind.Mushroom -> {
            drawRoundRect(tint(Color(0xFFFFF1DC)), topLeft = p(43f, 58f), size = Size(14 * u, 30 * u), cornerRadius = CornerRadius(5 * u))
            drawRoundRect(ink, topLeft = p(43f, 58f), size = Size(14 * u, 30 * u), cornerRadius = CornerRadius(5 * u), style = outline)
            drawArc(tint(Bright.Coral), 180f, 180f, true, topLeft = p(24f, 42f), size = Size(52 * u, 40 * u))
            drawArc(ink, 180f, 180f, true, topLeft = p(24f, 42f), size = Size(52 * u, 40 * u), style = outline)
            listOf(p(38f, 54f), p(52f, 48f), p(63f, 56f)).forEach { drawCircle(Color.White, radius = 3.5f * u, center = it) }
        }
        PlantKind.Sunflower -> {
            stem(u, 38f, tint(Stem))
            leaf(u, Offset(50f, 70f), 210f, tint(Stem))
            petals(u, Offset(50f, 34f), count = 10, distance = 13f, radius = 7f, colours = listOf(tint(Bright.Sunflower)), ink = ink)
            drawCircle(tint(Bark), radius = 9 * u, center = p(50f, 34f))
        }
        PlantKind.Cactus -> {
            val body = tint(Bright.Mint)
            drawRoundRect(body, topLeft = p(26f, 50f), size = Size(12 * u, 22 * u), cornerRadius = CornerRadius(6 * u))
            drawRoundRect(body, topLeft = p(62f, 44f), size = Size(12 * u, 20 * u), cornerRadius = CornerRadius(6 * u))
            drawRect(body, topLeft = p(32f, 64f), size = Size(10 * u, 8 * u))
            drawRect(body, topLeft = p(58f, 56f), size = Size(10 * u, 8 * u))
            drawRoundRect(body, topLeft = p(40f, 38f), size = Size(20 * u, 50 * u), cornerRadius = CornerRadius(10 * u))
            drawRoundRect(ink, topLeft = p(40f, 38f), size = Size(20 * u, 50 * u), cornerRadius = CornerRadius(10 * u), style = outline)
            drawCircle(tint(Bright.Pink), radius = 6 * u, center = p(50f, 36f))
            drawCircle(ink, radius = 6 * u, center = p(50f, 36f), style = outline)
        }
        PlantKind.Bush -> {
            drawCircle(tint(Bright.Violet), radius = 17 * u, center = p(36f, 70f))
            drawCircle(tint(Bright.Sky), radius = 17 * u, center = p(64f, 70f))
            drawCircle(tint(Color(0xFF9C86FF)), radius = 19 * u, center = p(50f, 56f))
            listOf(p(42f, 52f), p(58f, 60f), p(48f, 72f)).forEach { drawCircle(tint(Bright.Sunflower), radius = 3 * u, center = it) }
        }
        PlantKind.Tree -> {
            drawRect(tint(Bark), topLeft = p(45f, 52f), size = Size(10 * u, 36 * u))
            drawCircle(tint(Bright.Pink), radius = 26 * u, center = p(50f, 38f))
            drawCircle(ink, radius = 26 * u, center = p(50f, 38f), style = outline)
            listOf(p(40f, 30f), p(58f, 26f), p(62f, 44f), p(44f, 48f), p(52f, 36f)).forEach { drawCircle(Color.White, radius = 3.5f * u, center = it) }
        }
        PlantKind.QuestStar -> {
            stem(u, 44f, tint(Stem))
            val star = starPath(u, Offset(50f, 34f), outer = 17f, inner = 7.5f)
            drawPath(star, tint(Bright.Sunflower))
            drawPath(star, ink, style = outline)
        }
        PlantKind.RareBloom -> {
            stem(u, 44f, tint(Stem))
            petals(u, Offset(50f, 36f), count = 6, distance = 12f, radius = 8.5f, colours = Bright.cycle.map(tint), ink = ink)
            drawCircle(Color.White, radius = 6 * u, center = p(50f, 36f))
            val sparkle = starPath(u, Offset(76f, 18f), outer = 7f, inner = 2.5f, points = 4)
            drawPath(sparkle, tint(Bright.Sunflower))
        }
    }
}

private fun DrawScope.stem(u: Float, topY: Float, colour: Color) {
    drawLine(colour, Offset(50 * u, 88 * u), Offset(50 * u, topY * u), strokeWidth = 4 * u, cap = StrokeCap.Round)
}

private fun DrawScope.leaf(u: Float, base: Offset, degrees: Float, colour: Color) {
    rotate(degrees, pivot = Offset(base.x * u, base.y * u)) {
        drawOval(colour, topLeft = Offset(base.x * u, (base.y - 5) * u), size = Size(18 * u, 10 * u))
    }
}

private fun DrawScope.petals(u: Float, centre: Offset, count: Int, distance: Float, radius: Float, colours: List<Color>, ink: Color) {
    for (i in 0 until count) {
        val angle = 2 * PI * i / count
        val c = Offset((centre.x + distance * cos(angle).toFloat()) * u, (centre.y + distance * sin(angle).toFloat()) * u)
        drawCircle(colours[i % colours.size], radius = radius * u, center = c)
        drawCircle(ink, radius = radius * u, center = c, style = Stroke(width = 1.6f * u))
    }
}

private fun starPath(u: Float, centre: Offset, outer: Float, inner: Float, points: Int = 5) = Path().apply {
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) outer else inner
        val angle = -PI / 2 + PI * i / points
        val x = (centre.x + r * cos(angle).toFloat()) * u
        val y = (centre.y + r * sin(angle).toFloat()) * u
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
