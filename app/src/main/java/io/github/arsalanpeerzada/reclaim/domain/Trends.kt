package io.github.arsalanpeerzada.reclaim.domain

import kotlin.math.roundToInt

/** The design doc's primary metric: the trend in reels per day and the share of days within budget, 30-day rolling. */
data class MonthTrend(
    val daysTracked: Int,
    val percentWithinBudget: Int,
    val averagePerDay: Int,
    /** Average of the first seven days ever tracked, the starting point the trend is measured from. */
    val startingAverage: Int,
    /** Change in the daily average against the start, in percent (negative is fewer reels). */
    val changeFromStartPercent: Int?,
)

/**
 * Rolling 30-day trends, and graduation: the design doc counts a user who no longer needs the app as
 * a win, not churn. Thresholds are first guesses, like the doc's other defaults.
 */
object Trends {
    const val WINDOW_DAYS = 30
    const val MIN_DAYS = 7
    const val GRADUATE_PERCENT_WITHIN = 90

    /** At most half the user's usual day before Reclaim. */
    const val GRADUATE_FRACTION_OF_USUAL = 0.5

    /** [completedDays] in date order, today excluded. Null until a week has been tracked. */
    fun lastMonth(completedDays: List<DayOutcome>): MonthTrend? {
        if (completedDays.size < MIN_DAYS) return null
        val window = completedDays.takeLast(WINDOW_DAYS)
        val average = window.map { it.reels }.average()
        val start = completedDays.take(MIN_DAYS).map { it.reels }.average()
        return MonthTrend(
            daysTracked = window.size,
            percentWithinBudget = (window.count { it.withinBudget } * 100.0 / window.size).roundToInt(),
            averagePerDay = average.roundToInt(),
            startingAverage = start.roundToInt(),
            changeFromStartPercent = if (start > 0 && completedDays.size > MIN_DAYS) ((average - start) / start * 100).roundToInt() else null,
        )
    }

    /**
     * Ready to graduate: a full 30 days tracked, at least 90% of them within budget, and a daily
     * average at most half the usual day the user started from.
     */
    fun canGraduate(completedDays: List<DayOutcome>, typicalReelsPerDay: Int): Boolean {
        val trend = lastMonth(completedDays) ?: return false
        return trend.daysTracked >= WINDOW_DAYS &&
            trend.percentWithinBudget >= GRADUATE_PERCENT_WITHIN &&
            trend.averagePerDay <= typicalReelsPerDay * GRADUATE_FRACTION_OF_USUAL
    }
}
