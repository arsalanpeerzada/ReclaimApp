package io.github.arsalanpeerzada.reclaim.ui.reflection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arsalanpeerzada.reclaim.AppContainer
import io.github.arsalanpeerzada.reclaim.R
import io.github.arsalanpeerzada.reclaim.domain.Progress
import io.github.arsalanpeerzada.reclaim.domain.ReclaimedTime
import io.github.arsalanpeerzada.reclaim.domain.Reflections
import io.github.arsalanpeerzada.reclaim.domain.MonthTrend
import io.github.arsalanpeerzada.reclaim.domain.Streaks
import io.github.arsalanpeerzada.reclaim.domain.Trends
import io.github.arsalanpeerzada.reclaim.domain.WeeklyReflection
import io.github.arsalanpeerzada.reclaim.domain.Weeks
import io.github.arsalanpeerzada.reclaim.ui.components.CircleIconButton
import io.github.arsalanpeerzada.reclaim.ui.components.GameCard
import io.github.arsalanpeerzada.reclaim.ui.components.PillButton
import io.github.arsalanpeerzada.reclaim.ui.components.PipMood
import io.github.arsalanpeerzada.reclaim.ui.components.PipSays
import io.github.arsalanpeerzada.reclaim.ui.components.ScreenTitle
import io.github.arsalanpeerzada.reclaim.ui.components.StatTile
import io.github.arsalanpeerzada.reclaim.ui.components.TextLink
import io.github.arsalanpeerzada.reclaim.ui.share.SquadCard
import io.github.arsalanpeerzada.reclaim.ui.share.SquadWeek
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReflectionUiState(val reflection: WeeklyReflection?, val squad: SquadWeek?, val month: MonthTrend?)

class ReflectionViewModel(private val container: AppContainer) : ViewModel() {
    private val weekStart = Weeks.lastCompleted(container.reels.today())

    val state: StateFlow<ReflectionUiState?> = combine(
        container.reels.history,
        container.settings.settings,
        container.wellbeing.moods,
    ) { history, settings, moods ->
        val today = container.reels.today()
        val completed = Progress.days(history, today, settings.startDay ?: today, settings.dailyBudget).dropLast(1)
        val reflection = Reflections.build(completed, moods, weekStart, settings.dailyBudget)
        ReflectionUiState(
            reflection = reflection,
            month = Trends.lastMonth(completed),
            squad = reflection?.let {
                SquadWeek(
                    weekStart = it.weekStart,
                    daysWithinBudget = it.daysWithinBudget,
                    trackedDays = it.days.size,
                    streak = Streaks.compute(completed).current,
                    averagePerDay = it.averagePerDay,
                    minutesWonBack = ReclaimedTime.compute(it.days, settings.typicalReelsPerDay).minutes,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Applies the chosen budget (if it changed) and marks the week reflected. */
    fun finish(budget: Int?, onDone: () -> Unit) {
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            if (budget != null && budget != settings.dailyBudget) container.reels.updateGoals(budget, settings.typicalReelsPerDay)
            container.settings.markReflected(weekStart)
            onDone()
        }
    }
}

@Composable
fun ReflectionScreen(container: AppContainer, onBack: () -> Unit) {
    val viewModel = viewModel { ReflectionViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val palette = ReclaimTheme.palette

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CircleIconButton(R.drawable.ic_back, "Back", onBack)
            ScreenTitle("Your week")
        }
        val current = state ?: return@Column
        val r = current.reflection
        if (r == null) {
            PipSays(PipMood.Happy, "Nothing to look back on yet", "Your first weekly reflection arrives the Monday after your first full week.")
            return@Column
        }
        Text(
            "${r.weekStart.format(Short)} to ${r.weekStart.plusDays(6).format(Short)}",
            style = MaterialTheme.typography.labelMedium,
            color = palette.textMuted,
        )
        val (mood, headline) = when {
            r.daysWithinBudget >= 5 -> PipMood.Proud to "Brilliant week!"
            r.daysWithinBudget >= 3 -> PipMood.Happy to "A solid week."
            else -> PipMood.Calm to "A tough week, and that's okay."
        }
        GameCard(Modifier.fillMaxWidth(), colour = palette.softViolet) {
            PipSays(mood, headline, "${r.daysWithinBudget} of ${r.days.size} days within budget. Every week is a fresh start.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(R.drawable.ic_star, "${r.averagePerDay}", "reels a day on average", Bright.Sky, Modifier.weight(1f))
            StatTile(
                R.drawable.ic_flame,
                r.bestDay.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                "best day: ${r.bestDay.reels} reels",
                Bright.Coral,
                Modifier.weight(1f),
            )
        }
        r.changePercent?.let { change ->
            val text = when {
                change < 0 -> "${abs(change)}% fewer reels a day than the week before."
                change > 0 -> "$change% more reels a day than the week before. Next week is a new go."
                else -> "The same as the week before."
            }
            Text(text, style = MaterialTheme.typography.titleSmall, color = palette.text)
        }
        current.month?.let { month ->
            GameCard(Modifier.fillMaxWidth(), colour = palette.softSky, padding = 16.dp) {
                Text("The last ${month.daysTracked} days", style = MaterialTheme.typography.titleLarge, color = palette.text)
                Text(
                    "${month.percentWithinBudget}% of days within budget, about ${month.averagePerDay} reels a day." +
                        (
                            month.changeFromStartPercent?.let { change ->
                                when {
                                    change < 0 -> " That's ${abs(change)}% fewer than your first week (${month.startingAverage} a day)."
                                    change > 0 -> " That's $change% more than your first week (${month.startingAverage} a day)."
                                    else -> " The same as your first week."
                                }
                            } ?: ""
                            ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.text,
                )
            }
        }
        GameCard(Modifier.fillMaxWidth(), colour = palette.softPink, padding = 16.dp) {
            Text("Mood and scrolling", style = MaterialTheme.typography.titleLarge, color = palette.text)
            Text(r.moodInsight, style = MaterialTheme.typography.bodyMedium, color = palette.text)
        }

        GameCard(Modifier.fillMaxWidth(), padding = 16.dp) {
            Text("Next week's budget", style = MaterialTheme.typography.titleLarge, color = palette.text)
            val suggestion = r.suggestedBudget
            if (suggestion == null) {
                Text("${r.currentBudget} reels a day looks right for you.", style = MaterialTheme.typography.bodyMedium, color = palette.text)
                PillButton("Keep ${r.currentBudget}", { viewModel.finish(null, onBack) }, badge = Bright.Mint)
            } else {
                Text(
                    if (suggestion < r.currentBudget) {
                        "You nearly always stayed within ${r.currentBudget}. Ready for a little less?"
                    } else {
                        "${r.currentBudget} was hard to stick to. A slightly higher budget can make it doable; you can lower it again later."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.text,
                )
                PillButton("Try $suggestion a day", { viewModel.finish(suggestion, onBack) }, badge = Bright.Mint)
                TextLink("Keep ${r.currentBudget}", { viewModel.finish(null, onBack) }, Modifier.align(Alignment.CenterHorizontally))
            }
        }

        current.squad?.let { squad ->
            GameCard(Modifier.fillMaxWidth(), colour = palette.softMint, padding = 16.dp) {
                Text("Squad up", style = MaterialTheme.typography.titleLarge, color = palette.text)
                Text(
                    "Send your week to two to four friends and ask them to share theirs. Cheering each other on beats any leaderboard. " +
                        "The card only shows counts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.text,
                )
                PillButton("Share my week", { SquadCard.share(context, squad) }, badge = Bright.Sky)
            }
        }
    }
}

private val Short = DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault())
