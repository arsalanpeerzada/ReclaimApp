package io.github.arsalanpeerzada.reclaim.ui.quests

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arsalanpeerzada.reclaim.AppContainer
import io.github.arsalanpeerzada.reclaim.R
import io.github.arsalanpeerzada.reclaim.domain.Quest
import io.github.arsalanpeerzada.reclaim.domain.Quests
import io.github.arsalanpeerzada.reclaim.ui.components.ConfettiBurst
import io.github.arsalanpeerzada.reclaim.ui.components.FloatingNavClearance
import io.github.arsalanpeerzada.reclaim.ui.components.GameCard
import io.github.arsalanpeerzada.reclaim.ui.components.PipMood
import io.github.arsalanpeerzada.reclaim.ui.components.PipSays
import io.github.arsalanpeerzada.reclaim.ui.components.ScreenTitle
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuestItem(val quest: Quest, val done: Boolean, val surprise: Boolean)

data class QuestsUiState(val items: List<QuestItem>, val doneAllTime: Int)

class QuestsViewModel(private val container: AppContainer) : ViewModel() {
    private val today = MutableStateFlow(container.reels.today())

    val state: StateFlow<QuestsUiState?> = combine(container.quests.completed, today) { completed, today ->
        val doneToday = completed[today].orEmpty()
        QuestsUiState(
            items = Quests.forDay(today).map { QuestItem(it, it.id in doneToday, Quests.isSurprise(today, it.id)) },
            doneAllTime = completed.values.sumOf { it.size },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDone(questId: String, done: Boolean) {
        viewModelScope.launch { container.quests.setDone(questId, done) }
    }

    fun refreshDate() {
        today.value = container.reels.today()
    }
}

@Composable
fun QuestsScreen(container: AppContainer) {
    val viewModel = viewModel { QuestsViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }
    val current = state ?: return
    val palette = ReclaimTheme.palette
    val doneToday = current.items.count { it.done }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = FloatingNavClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Quests")
        GameCard(Modifier.fillMaxWidth(), colour = palette.softViolet) {
            when (doneToday) {
                0 -> PipSays(PipMood.Happy, "Swap a scroll for something real", "Do one of today's quests, then tick it off. Your word is enough.")
                Quests.PER_DAY -> PipSays(PipMood.Proud, "All three done!", "That's a proper day. New quests tomorrow.")
                else -> PipSays(PipMood.Happy, "$doneToday of ${Quests.PER_DAY} done", "Each one plants a star flower in the garden.")
            }
        }
        current.items.forEachIndexed { index, item ->
            QuestCard(item, colour = QuestColours[index % QuestColours.size], onToggle = { viewModel.setDone(item.quest.id, !item.done) })
        }
        Text(
            "Quests completed so far: ${current.doneAllTime}. Nothing is tracked: no location, no steps, no sensors.",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
        )
    }
}

private val QuestColours = listOf(Bright.Coral, Bright.Sky, Bright.Sunflower)

@Composable
private fun QuestCard(item: QuestItem, colour: Color, onToggle: () -> Unit) {
    val palette = ReclaimTheme.palette
    // Bursts only when the user ticks it now, not when the screen opens on a quest already done.
    var burst by remember { mutableIntStateOf(0) }
    val check by animateFloatAsState(
        if (item.done) 1f else 0.8f,
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    Box {
    GameCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).background(colour, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(painterResource(iconFor(item.quest.icon)), contentDescription = null, tint = Bright.Ink, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.quest.title, style = MaterialTheme.typography.titleMedium, color = palette.text)
                Text(item.quest.detail, style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
            }
            Surface(
                onClick = {
                    if (!item.done) burst++
                    onToggle()
                },
                shape = CircleShape,
                color = if (item.done) Bright.Mint else palette.track,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { scaleX = check; scaleY = check }
                    .semantics {
                        role = Role.Checkbox
                        stateDescription = if (item.done) "Done" else "Not done"
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_check),
                        contentDescription = "Mark ${item.quest.title} done",
                        tint = if (item.done) Bright.Ink else palette.textMuted,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
        AnimatedVisibility(visible = item.done) {
            Text(
                if (item.surprise) "Surprise! A rare rainbow bloom joined your garden." else "A star flower joined your garden.",
                style = MaterialTheme.typography.labelMedium,
                color = palette.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (item.surprise) palette.softPink else palette.softMint, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
    ConfettiBurst(
        trigger = burst.takeIf { it > 0 }?.let { "${item.quest.id}-$it" },
        modifier = Modifier.matchParentSize(),
        pieces = if (item.surprise) 70 else 32,
        colours = if (item.surprise) Bright.cycle else listOf(Bright.Sunflower, Bright.Mint, colour),
    )
    }
}

@DrawableRes
private fun iconFor(name: String): Int = when (name) {
    "route" -> R.drawable.ic_route
    "book" -> R.drawable.ic_book
    "stretch" -> R.drawable.ic_stretch
    "heart" -> R.drawable.ic_heart
    "sun" -> R.drawable.ic_sun
    "pen" -> R.drawable.ic_pen
    "cup" -> R.drawable.ic_cup
    "music" -> R.drawable.ic_music
    "wind" -> R.drawable.ic_wind
    "tidy" -> R.drawable.ic_tidy
    else -> R.drawable.ic_quest
}
