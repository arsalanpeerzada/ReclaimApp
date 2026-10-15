package io.github.arsalanpeerzada.reclaim.stats

import io.github.arsalanpeerzada.reclaim.counting.TrackedApp
import io.github.arsalanpeerzada.reclaim.domain.DayOutcome
import io.github.arsalanpeerzada.reclaim.domain.History
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Pause outcomes for one day. */
data class PauseCounts(val shown: Int, val stopped: Int)

/**
 * Exactly what one shared day contains: counts and outcomes, nothing about content, apps other than
 * Instagram and TikTok, times of day or the person. Mood is included only with the separate
 * wellbeing consent. These are the fields the admin dashboard and the Firestore rules expect.
 */
object StatsSummaries {
    fun day(
        day: DayOutcome,
        startDay: LocalDate,
        history: History,
        questsDone: Int,
        pauses: PauseCounts?,
        mood: Int?,
        includeWellbeing: Boolean,
    ): Map<String, Any?> {
        val byApp = history.reels[day.date].orEmpty()
        return buildMap {
            put("date", day.date.toString())
            put("dayIndex", ChronoUnit.DAYS.between(startDay, day.date).toInt())
            put("reels", day.reels)
            put("budget", day.budget)
            put("within", day.withinBudget)
            put("instagram", byApp[TrackedApp.INSTAGRAM] ?: 0)
            put("tiktok", byApp[TrackedApp.TIKTOK] ?: 0)
            put("quests", questsDone)
            put("pausesShown", pauses?.shown ?: 0)
            put("pausesStopped", pauses?.stopped ?: 0)
            if (includeWellbeing) put("mood", mood)
        }
    }

    /** Completed days after [lastUploaded] (all of them if null), oldest first. */
    fun pending(completedDays: List<DayOutcome>, lastUploaded: LocalDate?): List<DayOutcome> =
        completedDays.filter { lastUploaded == null || it.date.isAfter(lastUploaded) }
}
