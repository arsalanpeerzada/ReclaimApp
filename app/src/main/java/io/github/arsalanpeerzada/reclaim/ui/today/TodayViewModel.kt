package io.github.arsalanpeerzada.reclaim.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arsalanpeerzada.reclaim.AppContainer
import io.github.arsalanpeerzada.reclaim.data.Settings
import io.github.arsalanpeerzada.reclaim.domain.History
import io.github.arsalanpeerzada.reclaim.domain.MonthTrend
import io.github.arsalanpeerzada.reclaim.domain.Overview
import io.github.arsalanpeerzada.reclaim.domain.Trends
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import io.github.arsalanpeerzada.reclaim.domain.Progress
import io.github.arsalanpeerzada.reclaim.domain.Weeks
import io.github.arsalanpeerzada.reclaim.domain.Who5
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val overview: Overview,
    val typicalReelsPerDay: Int,
    /** Today's mood tag, 1 to 5, or null. */
    val mood: Int?,
    /** Last week has tracked days and hasn't been reflected on yet. */
    val reflectionDue: Boolean,
    val checkInDue: Boolean,
    val graduation: Graduation,
)

sealed interface Graduation {
    data object NotYet : Graduation

    /** A steady month well below the usual day: offer to graduate. */
    data class Ready(val trend: MonthTrend) : Graduation

    data class Graduated(val on: LocalDate) : Graduation
}

class TodayViewModel(private val container: AppContainer) : ViewModel() {
    // Refreshed on resume, so the screen rolls over to a new day without restarting the app.
    private val today = MutableStateFlow(container.reels.today())

    val state: StateFlow<TodayUiState?> = combine(
        container.reels.history,
        container.settings.settings,
        today,
        container.wellbeing.moods,
        container.wellbeing.who5,
    ) { history, settings, today, moods, who5 ->
        val startDay = settings.startDay ?: today
        val lastWeek = Weeks.lastCompleted(today)
        TodayUiState(
            overview = Progress.overview(
                history = history,
                today = today,
                startDay = startDay,
                currentBudget = settings.dailyBudget,
                typicalReelsPerDay = settings.typicalReelsPerDay,
            ),
            typicalReelsPerDay = settings.typicalReelsPerDay,
            mood = moods[today],
            reflectionDue = settings.lastReflectedWeek != lastWeek && !startDay.isAfter(lastWeek.plusDays(6)),
            checkInDue = Who5.isDue(who5, today),
            graduation = graduationFor(history, settings, today, startDay),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun graduationFor(history: History, settings: Settings, today: LocalDate, startDay: LocalDate): Graduation {
        settings.graduatedOn?.let { return Graduation.Graduated(it) }
        val asked = settings.graduationDismissedOn
        if (asked != null && ChronoUnit.DAYS.between(asked, today) < Trends.WINDOW_DAYS) return Graduation.NotYet
        val completed = Progress.days(history, today, startDay, settings.dailyBudget).dropLast(1)
        if (!Trends.canGraduate(completed, settings.typicalReelsPerDay)) return Graduation.NotYet
        return Trends.lastMonth(completed)?.let { Graduation.Ready(it) } ?: Graduation.NotYet
    }

    fun graduate() {
        viewModelScope.launch { container.settings.graduate(container.reels.today()) }
    }

    fun notYet() {
        viewModelScope.launch { container.settings.dismissGraduation(container.reels.today()) }
    }

    fun refreshDate() {
        today.value = container.reels.today()
    }

    fun setMood(mood: Int) {
        viewModelScope.launch { container.wellbeing.setMoodToday(mood) }
    }
}
