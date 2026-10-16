package io.github.arsalanpeerzada.reclaim.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.arsalanpeerzada.reclaim.R
import io.github.arsalanpeerzada.reclaim.domain.ReclaimedTime
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import kotlin.math.roundToInt

/** A rounded card with a slim edge underneath, like a game tile resting on the page. */
@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    colour: Color = ReclaimTheme.palette.card,
    radius: Dp = 28.dp,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val edge = ReclaimTheme.palette.cardEdge
    Column(
        modifier
            .drawBehind {
                drawRoundRect(edge, topLeft = Offset(0f, 2.dp.toPx()), size = size, cornerRadius = CornerRadius(radius.toPx()))
            }
            .background(colour, RoundedCornerShape(radius))
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

/** The main call to action: a dark pill with a coloured arrow badge. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Color = Bright.Sunflower,
    enabled: Boolean = true,
) {
    val palette = ReclaimTheme.palette
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    val nudge by animateDpAsState(if (pressed) 4.dp else 0.dp, label = "arrow")
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = palette.pill,
        contentColor = palette.onPill,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().height(60.dp).graphicsLayer { scaleX = scale; scaleY = scale }.alpha(if (enabled) 1f else 0.4f),
    ) {
        Row(Modifier.padding(start = 26.dp, end = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Box(Modifier.offset(x = nudge).size(46.dp).background(badge, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, tint = Bright.Ink, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = ReclaimTheme.palette.link)
    }
}

@Composable
fun CircleIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = ReclaimTheme.palette.card,
    size: Dp = 44.dp,
) {
    Surface(onClick = onClick, shape = CircleShape, color = container, contentColor = ReclaimTheme.palette.text, modifier = modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        }
    }
}

/** Onboarding progress: one coloured segment per step reached. */
@Composable
fun StepSegments(count: Int, reached: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().semantics { contentDescription = "Step $reached of $count" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(count) { i ->
            val colour = if (i < reached) Bright.cycle[i % Bright.cycle.size] else ReclaimTheme.palette.track
            Box(Modifier.weight(1f).height(8.dp).background(colour, CircleShape))
        }
    }
}

@Composable
fun Chip(text: String, container: Color, modifier: Modifier = Modifier, contentColour: Color = ReclaimTheme.palette.text) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = contentColour,
        modifier = modifier.background(container, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** A bright stat tile. Text on the bright colours is always ink, in light and dark mode. */
@Composable
fun StatTile(
    @DrawableRes icon: Int,
    value: String,
    label: String,
    colour: Color,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    Column(
        modifier.background(colour, RoundedCornerShape(22.dp)).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = Bright.Ink, modifier = Modifier.size(22.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            RollingNumber(value, MaterialTheme.typography.headlineMedium, Bright.Ink)
            if (unit != null) {
                Text(unit, style = MaterialTheme.typography.titleLarge, color = Bright.Ink, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Bright.Ink)
    }
}

/**
 * Pick a number with − and +, a colourful tick strip and a slider, as in the budget mockup.
 * Values snap to [step] within [range].
 */
@Composable
fun StepperCard(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    endHint: String = "${range.last}",
) {
    val palette = ReclaimTheme.palette
    fun set(v: Int) = onValueChange(v.coerceIn(range))
    GameCard(modifier.fillMaxWidth(), padding = 20.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(R.drawable.ic_minus, "$step fewer", { set(value - step) }, container = palette.softCoral, size = 56.dp)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                RollingNumber("$value", MaterialTheme.typography.displayLarge, palette.text)
                Text(unit, style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
            }
            CircleIconButton(R.drawable.ic_plus, "$step more", { set(value + step) }, container = palette.softMint, size = 56.dp)
        }
        val fraction = (value - range.first).toFloat() / (range.last - range.first).coerceAtLeast(1)
        Row(Modifier.fillMaxWidth().height(26.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            repeat(TICKS) { i ->
                val on = (i + 0.5f) / TICKS <= fraction
                val colour by animateColorAsState(if (on) Bright.cycle[i % Bright.cycle.size] else palette.track, tween(220), label = "tick")
                val height by animateDpAsState((if (on) 10 + i * 0.8f else 8f).dp, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tick height")
                Box(
                    Modifier
                        .weight(1f)
                        .height(height)
                        .background(colour, CircleShape),
                )
            }
        }
        val positions = (range.last - range.first) / step
        Slider(
            value = value.toFloat(),
            onValueChange = { raw -> set(((raw - range.first) / step).roundToInt() * step + range.first) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = (positions - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = Bright.Violet,
                activeTrackColor = Bright.Violet,
                inactiveTrackColor = palette.track,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.semantics { contentDescription = label },
        )
        Row(Modifier.fillMaxWidth()) {
            Text("${range.first}", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
            Spacer(Modifier.weight(1f))
            Text(endHint, style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
        }
    }
}

private const val TICKS = 20

/** Pip with a speech bubble. */
@Composable
fun PipSays(mood: PipMood, headline: String, detail: String, modifier: Modifier = Modifier, pipSize: Dp = 52.dp) =
    PipSays(PipLook.of(mood), headline, detail, modifier, pipSize)

@Composable
fun PipSays(look: PipLook, headline: String, detail: String, modifier: Modifier = Modifier, pipSize: Dp = 52.dp) {
    val palette = ReclaimTheme.palette
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pip(look, Modifier.width(pipSize).height(pipSize * 130 / 120))
        Column(
            Modifier
                .weight(1f)
                .background(palette.softViolet, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(headline, style = MaterialTheme.typography.titleSmall, color = palette.text)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = palette.text)
        }
    }
}

/** An icon badge next to a short title and explanation. */
@Composable
fun NoticeCard(
    @DrawableRes icon: Int,
    badge: Color,
    container: Color,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val palette = ReclaimTheme.palette
    Row(
        modifier.fillMaxWidth().background(container, RoundedCornerShape(24.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(badge, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = null, tint = Bright.Ink, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = palette.text)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = palette.text)
        }
    }
}

/**
 * The transparent-permissions promise from the design doc: say exactly what the service reads,
 * ignores and keeps, before the user grants it.
 */
@Composable
fun PermissionCards(modifier: Modifier = Modifier) {
    val palette = ReclaimTheme.palette
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NoticeCard(
            R.drawable.ic_eye, Bright.Mint, palette.softMint, "It reads",
            "Which app is open, by name only, so the live counter shows just in Instagram and TikTok. " +
                "And in those two apps only: which full-screen reel is showing, and the size of the scroller.",
        )
        NoticeCard(
            R.drawable.ic_eye_off, Bright.Pink, palette.softPink, "It ignores",
            "Everything inside other apps. Text, messages, captions, images and video, in any app. Anything you type, including passwords.",
        )
        NoticeCard(
            R.drawable.ic_pause, Bright.Violet, palette.softViolet, "It can show",
            "A live counter at the top of Instagram and TikTok, a short mindful pause when you near your budget, and, " +
                "only if you turn it on, a lock once the budget is used. All switchable in Settings.",
        )
        NoticeCard(
            R.drawable.ic_phone, Bright.Sky, palette.softSky, "It keeps",
            "Daily totals per app, on this phone. Nothing leaves it unless you choose to share anonymous stats.",
        )
        Text(
            "Find Reclaim under Downloaded apps and switch it on. Leave its shortcut switch off: that only adds Android's own " +
                "floating button, which Reclaim doesn't need. If Android says the setting is restricted, open Reclaim's " +
                "App info, tap the three-dot menu and choose Allow restricted settings.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
        )
    }
}

/** Exactly what anonymous stats sharing sends, and what it never does. */
@Composable
fun ShareDetails(modifier: Modifier = Modifier) {
    val palette = ReclaimTheme.palette
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NoticeCard(
            R.drawable.ic_check, Bright.Mint, palette.softMint, "What's shared, once a day",
            "Reel totals for Instagram and TikTok, your budget and whether you stayed within it, quests ticked off, " +
                "how mindful pauses went, your level and streak. Under an anonymous id, not your name.",
        )
        NoticeCard(
            R.drawable.ic_eye_off, Bright.Pink, palette.softPink, "Never shared",
            "What you watch, other apps, times of day, your name, email, contacts, location or device id.",
        )
        NoticeCard(
            R.drawable.ic_heart, Bright.Violet, palette.softViolet, "Only if you also say yes",
            "Your daily mood tags and wellbeing check-in scores. These are about your health, so they get their own switch.",
        )
    }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier, textAlign: TextAlign? = null) {
    Text(text, style = MaterialTheme.typography.headlineLarge, color = ReclaimTheme.palette.text, modifier = modifier, textAlign = textAlign)
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = ReclaimTheme.palette.textMuted, modifier = modifier)
}

/** Budget options never go below 5 or above the user's typical day. */
fun budgetRange(typical: Int): IntRange = 5..typical.coerceAtLeast(10)

fun suggestedBudget(typical: Int): Int = ((typical / 2) / 5 * 5).coerceIn(budgetRange(typical))

/** Reels to whole minutes, at the design doc's 5 seconds a reel. */
fun reelMinutes(reels: Int): Int = reels * ReclaimedTime.SECONDS_PER_REEL / 60
