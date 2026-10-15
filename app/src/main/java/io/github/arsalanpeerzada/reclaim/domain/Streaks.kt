package io.github.arsalanpeerzada.reclaim.domain

import java.time.LocalDate

data class StreakState(
    /** Consecutive completed days within budget, with frozen days bridging the gaps. */
    val current: Int,
    val best: Int,
    val freezesLeft: Int,
    /** Days over budget that a freeze covered. */
    val frozenDays: Set<LocalDate>,
    /** Further days under budget needed to earn the next freeze. */
    val daysToNextFreeze: Int,
    /** Every completed day within budget, streak or not: what levels are measured in. */
    val daysWithinBudget: Int,
)

/**
 * Streaks with freezes: a day over budget spends a freeze instead of breaking the streak, which
 * avoids the "I broke it, why bother" collapse. The streak is always recomputed from history, so
 * there is no stored state to drift.
 */
object Streaks {
    const val STARTING_FREEZES = 1
    const val MAX_FREEZES = 2
    const val DAYS_PER_FREEZE = 7

    /** [completedDays] in date order, not including today. */
    fun compute(completedDays: List<DayOutcome>): StreakState {
        var current = 0
        var best = 0
        var freezes = STARTING_FREEZES
        var towardsNextFreeze = 0
        var within = 0
        val frozen = mutableSetOf<LocalDate>()

        for (day in completedDays) {
            when {
                day.withinBudget -> {
                    within++
                    current++
                    best = maxOf(best, current)
                    towardsNextFreeze++
                    if (towardsNextFreeze == DAYS_PER_FREEZE) {
                        freezes = minOf(freezes + 1, MAX_FREEZES)
                        towardsNextFreeze = 0
                    }
                }
                freezes > 0 -> {
                    freezes--
                    frozen += day.date
                }
                else -> {
                    current = 0
                    towardsNextFreeze = 0
                }
            }
        }
        return StreakState(
            current = current,
            best = best,
            freezesLeft = freezes,
            frozenDays = frozen,
            daysToNextFreeze = DAYS_PER_FREEZE - towardsNextFreeze,
            daysWithinBudget = within,
        )
    }
}
