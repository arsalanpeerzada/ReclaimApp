package io.github.arsalanpeerzada.reclaim.ui.world

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arsalanpeerzada.reclaim.AppContainer
import io.github.arsalanpeerzada.reclaim.domain.Plant
import io.github.arsalanpeerzada.reclaim.domain.PlantKind
import io.github.arsalanpeerzada.reclaim.domain.PlantSource
import io.github.arsalanpeerzada.reclaim.domain.Progress
import io.github.arsalanpeerzada.reclaim.domain.Seedling
import io.github.arsalanpeerzada.reclaim.domain.World
import io.github.arsalanpeerzada.reclaim.domain.WorldBuilder
import io.github.arsalanpeerzada.reclaim.ui.components.Chip
import io.github.arsalanpeerzada.reclaim.ui.components.FloatingNavClearance
import io.github.arsalanpeerzada.reclaim.ui.components.GameCard
import io.github.arsalanpeerzada.reclaim.ui.components.PipMood
import io.github.arsalanpeerzada.reclaim.ui.components.PipSays
import io.github.arsalanpeerzada.reclaim.ui.components.ScreenTitle
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class WorldViewModel(private val container: AppContainer) : ViewModel() {
    private val today = MutableStateFlow(container.reels.today())

    val world: StateFlow<World?> = combine(
        container.reels.history,
        container.settings.settings,
        container.quests.completed,
        today,
    ) { history, settings, quests, today ->
        val days = Progress.days(history, today, settings.startDay ?: today, settings.dailyBudget)
        WorldBuilder.build(completedDays = days.dropLast(1), today = days.last(), quests = quests)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refreshDate() {
        today.value = container.reels.today()
    }
}

@Composable
fun WorldScreen(container: AppContainer) {
    val viewModel = viewModel { WorldViewModel(container) }
    val world by viewModel.world.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }
    val current = world ?: return
    // Newest first, so tonight's plot and recent growth sit at the top.
    val plants = current.plants.asReversed()

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = FloatingNavClearance),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        fullWidth {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScreenTitle("Pip's garden")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(if (current.plants.size == 1) "1 plant" else "${current.plants.size} plants", ReclaimTheme.palette.softMint)
                    if (current.wilting > 0) Chip("${current.wilting} drooping", ReclaimTheme.palette.softCoral)
                }
            }
        }
        fullWidth { Hero(current) }
        item { TonightPlot(current.today) }
        items(plants) { plant -> PlantPlot(plant) }
        fullWidth {
            GameCard(Modifier.fillMaxWidth(), colour = ReclaimTheme.palette.softViolet, padding = 16.dp) {
                Text("How it grows", style = MaterialTheme.typography.titleLarge, color = ReclaimTheme.palette.text)
                Text(
                    "Every day within budget plants something new, and every ${WorldBuilder.TREE_EVERY}th is a blossom tree. " +
                        "A day over makes the newest plant droop; it perks up after your next good day. Nothing is ever taken away. " +
                        "Quests add star flowers, and now and then a surprise rare bloom.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ReclaimTheme.palette.text,
                )
            }
        }
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun Hero(world: World) {
    GameCard(Modifier.fillMaxWidth(), colour = ReclaimTheme.palette.softSky) {
        when {
            world.today == Seedling.Resting -> PipSays(
                PipMood.Calm, "The garden is resting",
                "Your newest plant is drooping a little. It perks up after your next day within budget.",
            )
            world.plants.isEmpty() -> PipSays(
                PipMood.Happy, "Your garden starts tonight",
                "Stay within budget today and the first plant grows at midnight.",
            )
            else -> PipSays(
                PipMood.Happy, "Something new blooms tonight",
                "Stay within budget today and another plant joins the garden at midnight.",
            )
        }
    }
}

@Composable
private fun TonightPlot(state: Seedling) {
    Plot(
        label = "Tonight",
        description = if (state == Seedling.Growing) "Tonight's plot: a sprout, growing" else "Tonight's plot: resting",
        highlight = true,
    ) {
        SeedlingArt(state, Modifier.fillMaxSize())
    }
}

@Composable
private fun PlantPlot(plant: Plant) {
    val name = when (plant.kind) {
        PlantKind.Tulip -> "Tulip"
        PlantKind.Daisy -> "Daisy"
        PlantKind.Mushroom -> "Mushroom"
        PlantKind.Sunflower -> "Sunflower"
        PlantKind.Cactus -> "Cactus"
        PlantKind.Bush -> "Berry bush"
        PlantKind.Tree -> "Blossom tree"
        PlantKind.QuestStar -> "Star flower"
        PlantKind.RareBloom -> "Rare rainbow bloom"
    }
    val why = when (plant.source) {
        PlantSource.BudgetDay -> "for a day within budget"
        PlantSource.Quest -> "for a quest"
        PlantSource.Surprise -> "a surprise for a quest"
    }
    Plot(
        label = plant.planted.format(PlotDate),
        description = "$name, ${plant.planted.format(PlotDate)}, $why${if (plant.wilted) ", drooping" else ""}",
        highlight = false,
    ) {
        PlantArt(plant.kind, plant.wilted, Modifier.fillMaxSize())
    }
}

private val PlotDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

@Composable
private fun Plot(label: String, description: String, highlight: Boolean, art: @Composable () -> Unit) {
    val palette = ReclaimTheme.palette
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.9f)
                .clip(RoundedCornerShape(18.dp))
                .background(if (highlight) palette.softViolet else palette.card),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(Modifier.fillMaxWidth().height(14.dp).background(palette.soil))
            Box(Modifier.fillMaxSize().padding(4.dp)) { art() }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (highlight) palette.text else palette.textMuted)
    }
}
