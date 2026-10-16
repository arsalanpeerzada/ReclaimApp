package io.github.arsalanpeerzada.reclaim.ui.pause

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.arsalanpeerzada.reclaim.domain.CommitmentLock
import io.github.arsalanpeerzada.reclaim.domain.LockState
import io.github.arsalanpeerzada.reclaim.ui.components.PillButton
import io.github.arsalanpeerzada.reclaim.ui.components.Pip
import io.github.arsalanpeerzada.reclaim.ui.components.PipLook
import io.github.arsalanpeerzada.reclaim.ui.components.TextLink
import io.github.arsalanpeerzada.reclaim.ui.components.popIn
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import kotlinx.coroutines.delay

/**
 * The commitment lock, shown over Instagram and TikTok once the budget is used and the user has
 * switched the lock on. It reminds them it was their own choice; one emergency unlock a day appears
 * after a wait.
 */
@Composable
fun LockScreen(state: LockState, onLeave: () -> Unit, onUnlock: () -> Unit) {
    val palette = ReclaimTheme.palette
    var waited by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.canUnlock) {
        while (state.canUnlock && waited < CommitmentLock.WAIT_SECONDS) {
            delay(1_000)
            waited++
        }
    }

    Box(Modifier.fillMaxSize().background(Bright.Ink.copy(alpha = 0.94f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .padding(24.dp)
                .popIn()
                .fillMaxWidth()
                .background(palette.ground, RoundedCornerShape(32.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Pip(PipLook.forBudget(state.reels.toFloat() / state.budget.coerceAtLeast(1)), Modifier.width(80.dp).height(86.dp))
            Text("Locked until tomorrow", style = MaterialTheme.typography.headlineMedium, color = palette.text, textAlign = TextAlign.Center)
            Text(
                "You chose to lock Instagram and TikTok once your budget of ${state.budget} reels is used. " +
                    "That's today done. See you tomorrow.",
                style = MaterialTheme.typography.bodyLarge,
                color = palette.textMuted,
                textAlign = TextAlign.Center,
            )
            PillButton("Back to home screen", onLeave, badge = Bright.Mint)
            if (state.canUnlock) {
                val left = CommitmentLock.WAIT_SECONDS - waited
                if (left > 0) {
                    Text(
                        "Emergency unlock available in $left s",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textMuted,
                    )
                }
                AnimatedVisibility(visible = left <= 0, enter = fadeIn()) {
                    TextLink("Unlock ${CommitmentLock.EXTRA_REELS} more reels (once today)", onUnlock)
                }
            } else {
                Text(
                    "Today's emergency unlock is used.",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted,
                )
            }
        }
    }
}
