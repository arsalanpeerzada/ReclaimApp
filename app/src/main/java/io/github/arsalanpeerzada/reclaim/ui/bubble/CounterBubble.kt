package io.github.arsalanpeerzada.reclaim.ui.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.arsalanpeerzada.reclaim.ui.components.BudgetMeter
import io.github.arsalanpeerzada.reclaim.ui.components.Pip
import io.github.arsalanpeerzada.reclaim.ui.components.PipLook
import io.github.arsalanpeerzada.reclaim.ui.components.pipLine
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright

data class BubbleState(val reels: Int, val budget: Int) {
    val fraction: Float get() = if (budget > 0) reels.toFloat() / budget else 1f
}

/**
 * The live counter pill: Pip (whose look follows the budget), today's count and the budget meter.
 * The meter moves with every reel and keeps going past the budget; the pill gives a small bump when
 * a reel counts. A tap opens Pip's line; another tap closes it. Always ink-dark, so it reads over any video.
 */
@Composable
fun CounterBubble(state: BubbleState) {
    var expanded by remember { mutableStateOf(false) }
    val bump = remember { Animatable(1f) }
    var lastReels by remember { mutableStateOf(state.reels) }
    LaunchedEffect(state.reels) {
        if (state.reels > lastReels) {
            bump.snapTo(1.12f)
            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        lastReels = state.reels
    }
    val fraction by animateFloatAsState(state.fraction, tween(450), label = "meter")
    val over = state.reels > state.budget
    val line = pipLine(state.reels, state.budget)

    Column(
        Modifier
            // Room around the pill so the bump never touches the edge of its window.
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .scale(bump.value)
            .clip(RoundedCornerShape(22.dp))
            .background(Bright.Ink.copy(alpha = 0.92f))
            .clickable { expanded = !expanded }
            .semantics { contentDescription = "${state.reels} of ${state.budget} reels today. ${line.headline}" }
            .animateContentSize()
            .padding(start = 6.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pip(PipLook.forBudget(state.fraction), Modifier.width(28.dp).height(30.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${state.reels}", style = MaterialTheme.typography.titleLarge, color = if (over) Bright.Coral else Color.White)
                Text(
                    " / ${state.budget}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            Meter(fraction)
        }
        AnimatedVisibility(expanded) {
            Text(
                "${line.headline} ${line.detail}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 3.dp, bottom = 2.dp).width(220.dp),
            )
        }
    }
}

/** Ten small bars filling from the bottom; the overflow lap rises over them in plum. */
@Composable
private fun Meter(fraction: Float) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        repeat(BudgetMeter.SEGMENTS) { i ->
            Box(
                Modifier
                    .width(5.dp)
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                val first = BudgetMeter.firstLap(fraction, i)
                val second = BudgetMeter.secondLap(fraction, i)
                if (first > 0f) Box(Modifier.fillMaxWidth().fillMaxHeight(first).background(BudgetMeter.colours[i]))
                // The pill is always dark, so the second lap uses the lifted plum.
                if (second > 0f) Box(Modifier.fillMaxWidth().fillMaxHeight(second).background(BudgetMeter.OverflowOnDark))
            }
        }
    }
}
