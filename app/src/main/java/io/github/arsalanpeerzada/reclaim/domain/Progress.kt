package io.github.arsalanpeerzada.reclaim.domain

import io.github.arsalanpeerzada.reclaim.counting.TrackedApp
import java.time.LocalDate

/** Everything recorded so far, as stored. */
data class History(
    val reels: Map<LocalDate, Map<TrackedApp, Int>>,
    val budgets: Map<LocalDate, Int>,
)

data class DayOutcome(val date: LocalDate, val reels: Int, val budget: Int) {
    val withinBudget: Boolean get() = reels <= budget
}

data class Overview(
    val today: DayOutcome,
    val todayByApp: Map<TrackedApp, Int>,
    /** Today's share of the budget, which can pass 1.0. */
    val todayFraction: Float,
    val streak: StreakState,
    val level: Level,
    /** The last seven days, oldest first, ending with today; days before counting started are left out. */
    val lastSevenDays: List<DayOutcome>,
    val reclaimed: Reclaimed,
)

object Progress {
    /** The share of the budget at which the gentle "getting close" state starts (design doc default). */
    const val NUDGE_FRACTION = 0.8f

    /** Every day from [startDay] to [today] inclusive, oldest first, with empty days filled in. */
    fun days(history: History, today: LocalDate, startDay: LocalDate, currentBudget: Int): List<DayOutcome> {
        val start = minOf(startDay, today)
        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .map { date ->
                DayOutcome(
                    date = date,
                    reels = history.reels[date]?.values?.sum() ?: 0,
                    budget = history.budgets[date] ?: currentBudget,
                )
            }
            .toList()
    }

    fun overview(
        history: History,
        today: LocalDate,
        startDay: LocalDate,
        currentBudget: Int,
        typicalReelsPerDay: Int,
    ): Overview {
        val days = days(history, today, startDay, currentBudget)
        val todayOutcome = days.last()
        val completed = days.dropLast(1)
        val streak = Streaks.compute(completed)
        return Overview(
            today = todayOutcome,
            todayByApp = history.reels[today].orEmpty(),
            todayFraction = if (todayOutcome.budget > 0) todayOutcome.reels.toFloat() / todayOutcome.budget else 1f,
            streak = streak,
            level = Levels.of(streak.daysWithinBudget),
            lastSevenDays = days.takeLast(7),
            reclaimed = ReclaimedTime.compute(completed, typicalReelsPerDay),
        )
    }
}
