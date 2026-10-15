package io.github.arsalanpeerzada.reclaim.domain

import java.time.LocalDate

enum class PauseStage {
    /** 80% of the budget: "Still choosing this?" */
    Nudge,

    /** The budget is used up. */
    BudgetReached,
}

data class PauseRequest(val stage: PauseStage, val reels: Int, val budget: Int)

/**
 * Decides when to show the mindful pause: friction at the moment of temptation (Fogg), at most once per
 * stage per day so it never nags. Not a block: the user can always keep watching.
 */
class PauseTracker {
    private var day: LocalDate? = null
    private var shown: PauseStage? = null

    fun onReel(day: LocalDate, reelsToday: Int, budget: Int): PauseRequest? {
        if (day != this.day) {
            this.day = day
            shown = null
        }
        if (budget <= 0) return null
        val stage = when {
            reelsToday >= budget -> PauseStage.BudgetReached
            reelsToday * 5 >= budget * 4 -> PauseStage.Nudge
            else -> return null
        }
        val previous = shown
        if (previous != null && stage.ordinal <= previous.ordinal) return null
        shown = stage
        return PauseRequest(stage, reelsToday, budget)
    }
}
