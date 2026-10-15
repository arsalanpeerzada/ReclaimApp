package io.github.arsalanpeerzada.reclaim.ui.pause

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.arsalanpeerzada.reclaim.domain.PauseRequest
import io.github.arsalanpeerzada.reclaim.domain.PauseStage
import io.github.arsalanpeerzada.reclaim.ui.components.PillButton
import io.github.arsalanpeerzada.reclaim.ui.components.Pip
import io.github.arsalanpeerzada.reclaim.ui.components.PipLook
import io.github.arsalanpeerzada.reclaim.ui.components.TextLink
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme

private const val BREATH_MILLIS = 4_000

/**
 * "Still choosing this?" plus one breath. The choices appear once the breath is done, which is the
 * whole intervention: a moment off autopilot, then the user decides. No guilt in the wording.
 */
@Composable
fun MindfulPauseScreen(request: PauseRequest, onDone: () -> Unit, onKeepWatching: () -> Unit) {
    val palette = ReclaimTheme.palette
    val scale = remember { Animatable(0.6f) }
    var breathingOut by remember { mutableStateOf(false) }
    var breathDone by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(BREATH_MILLIS, easing = FastOutSlowInEasing))
        breathingOut = true
        scale.animateTo(0.6f, tween(BREATH_MILLIS, easing = FastOutSlowInEasing))
        breathDone = true
    }

    val budgetReached = request.stage == PauseStage.BudgetReached
    Box(Modifier.fillMaxSize().background(Bright.Ink.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .background(palette.ground, RoundedCornerShape(32.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Pip(PipLook.forBudget(request.reels.toFloat() / request.budget.coerceAtLeast(1)), Modifier.width(72.dp).height(78.dp))
            Text(
                if (budgetReached) "That's today's budget" else "Still choosing this?",
                style = MaterialTheme.typography.headlineMedium,
                color = palette.text,
                textAlign = TextAlign.Center,
            )
            Text(
                if (budgetReached) {
                    "You've watched ${request.reels} reels, the budget you set. Keep going if you want; it's your call."
                } else {
                    "${request.reels} of ${request.budget} reels today. Take one breath, then decide."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textMuted,
                textAlign = TextAlign.Center,
            )
            Canvas(Modifier.size(150.dp)) {
                drawCircle(palette.softViolet)
                drawCircle(Bright.Violet.copy(alpha = 0.35f), radius = size.minDimension / 2 * scale.value)
                drawCircle(Bright.Violet, radius = size.minDimension / 2 * scale.value * 0.55f)
            }
            Text(
                when {
                    breathDone -> "Nice. Your call."
                    breathingOut -> "Breathe out..."
                    else -> "Breathe in..."
                },
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
            )
            AnimatedVisibility(visible = breathDone, enter = fadeIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PillButton("I'm done for now", onDone, badge = Bright.Mint)
                    TextLink("Keep watching", onKeepWatching)
                }
            }
        }
    }
}
