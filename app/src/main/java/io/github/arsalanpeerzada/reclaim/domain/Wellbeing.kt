package io.github.arsalanpeerzada.reclaim.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** Weeks start on Monday. */
object Weeks {
    fun startOf(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** The Monday of the last full week before [today]'s. */
    fun lastCompleted(today: LocalDate): LocalDate = startOf(today).minusWeeks(1)

    fun contains(weekStart: LocalDate, date: LocalDate) = !date.isBefore(weekStart) && date.isBefore(weekStart.plusWeeks(1))
}

/** Daily mood tags, 1 (low) to 5 (great). */
object Moods {
    const val LOW_UP_TO = 2
    val labels = listOf("Low", "Meh", "Okay", "Good", "Great")
}

data class WeeklyReflection(
    val weekStart: LocalDate,
    /** Days of the week that were tracked, oldest first. */
    val days: List<DayOutcome>,
    val totalReels: Int,
    val averagePerDay: Int,
    val daysWithinBudget: Int,
    /** Change in the daily average against the week before, in percent; null without a week before. */
    val changePercent: Int?,
    /** The day with the fewest reels. */
    val bestDay: DayOutcome,
    val moodInsight: String,
    val currentBudget: Int,
    /** A gentler or more doable budget, or null when the current one looks right. Only ever a suggestion. */
    val suggestedBudget: Int?,
)

/** The weekly reflection: look back, notice a pattern, choose next week's budget. */
object Reflections {
    /** How far back mood and reels are compared: one week has too few days to say anything. */
    private const val INSIGHT_DAYS = 28L

    /** [completedDays] in date order, today excluded. Null when the week has no tracked days. */
    fun build(completedDays: List<DayOutcome>, moods: Map<LocalDate, Int>, weekStart: LocalDate, currentBudget: Int): WeeklyReflection? {
        val week = completedDays.filter { Weeks.contains(weekStart, it.date) }
        if (week.isEmpty()) return null
        val previous = completedDays.filter { Weeks.contains(weekStart.minusWeeks(1), it.date) }

        val average = week.map { it.reels }.average()
        val previousAverage = if (previous.isEmpty()) null else previous.map { it.reels }.average()
        val change = previousAverage?.takeIf { it > 0 }?.let { ((average - it) / it * 100).roundToInt() }

        val within = week.count { it.withinBudget }
        val insightFrom = weekStart.plusWeeks(1).minusDays(INSIGHT_DAYS)
        return WeeklyReflection(
            weekStart = weekStart,
            days = week,
            totalReels = week.sumOf { it.reels },
            averagePerDay = average.roundToInt(),
            daysWithinBudget = within,
            changePercent = change,
            bestDay = week.minBy { it.reels },
            moodInsight = moodInsight(completedDays.filter { !it.date.isBefore(insightFrom) && it.date.isBefore(weekStart.plusWeeks(1)) }, moods),
            currentBudget = currentBudget,
            suggestedBudget = suggestBudget(within, week.size, currentBudget),
        )
    }

    /** Mood tag against reels, shown back to the user (the design doc's weekly insight). */
    fun moodInsight(days: List<DayOutcome>, moods: Map<LocalDate, Int>): String {
        val tagged = days.mapNotNull { day -> moods[day.date]?.let { day to it } }
        val low = tagged.filter { it.second <= Moods.LOW_UP_TO }.map { it.first.reels }
        val other = tagged.filter { it.second > Moods.LOW_UP_TO }.map { it.first.reels }
        if (low.size < 2 || other.size < 2) {
            return "Tag your mood on the Today screen for a few days and you'll see how it links to your scrolling."
        }
        val otherAverage = other.average()
        val ratio = if (otherAverage > 0) low.average() / otherAverage else Double.POSITIVE_INFINITY
        return when {
            ratio >= 1.25 -> "You scrolled ${times(ratio)} as much on low-mood days. A quest might help on those days."
            ratio <= 0.8 -> "You scrolled less on low-mood days than on good ones."
            else -> "Your mood and your scrolling didn't seem linked this month."
        }
    }

    private fun times(ratio: Double): String {
        if (ratio.isInfinite()) return "far more than"
        val rounded = (ratio * 10).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) "${rounded.toInt()}x" else "${rounded}x"
    }

    /**
     * Nearly every day within budget: offer 10% less. Fewer than half: offer 10% more, so the goal stays
     * doable. Rounded to 5, never below 5. Needs at least four tracked days to say anything.
     */
    fun suggestBudget(within: Int, tracked: Int, current: Int): Int? {
        if (tracked < 4) return null
        val fraction = within.toDouble() / tracked
        val suggestion = when {
            fraction >= 6.0 / 7 -> (current * 0.9 / 5).roundToInt() * 5
            fraction < 0.5 -> (current * 1.1 / 5).roundToInt() * 5
            else -> return null
        }.coerceAtLeast(5)
        return suggestion.takeIf { it != current }
    }
}

/**
 * The WHO-5 Well-Being Index (1998 version): five statements about the last two weeks, each scored 0
 * to 5. The raw total (0 to 25) times 4 gives a percentage score; lower means poorer wellbeing. It is
 * a screening measure, never a diagnosis, and the app only shows it back to the user.
 *
 * Checked on 16 October 2026 against the official English questionnaire at
 * psykiatri-regionh.dk/who-5 ("WHO-5 questionaire - English.pdf"): wording, answer labels, scoring,
 * interpretation (raw score below 13, or any item answered 0 or 1, indicates poor wellbeing) and
 * monitoring change (a 10% difference is significant). The site states the copyright is now held by
 * WHO, with questions about use to WHO-5@who.int. Deviations from the official wording must be marked.
 */
object Who5 {
    val statements = listOf(
        "I have felt cheerful and in good spirits",
        "I have felt calm and relaxed",
        "I have felt active and vigorous",
        "I woke up feeling fresh and rested",
        "My daily life has been filled with things that interest me",
    )

    /** Answer labels with their scores, most to least. */
    val answers = listOf(
        "All of the time" to 5,
        "Most of the time" to 4,
        "More than half of the time" to 3,
        "Less than half of the time" to 2,
        "Some of the time" to 1,
        "At no time" to 0,
    )

    /** The official instruction, word for word. */
    const val INSTRUCTION = "Please indicate for each of the five statements which is closest to how you have been " +
        "feeling over the last two weeks. Notice that higher numbers mean better well-being."

    /** Official interpretation: a raw score below this indicates poor wellbeing. */
    const val LOW_RAW_BELOW = 13

    /** Official monitoring change: a difference of this many percentage points is significant. */
    const val SIGNIFICANT_CHANGE = 10

    /** Kept for screens that only have the percentage: raw below 13 is the same as 48% or less. */
    const val LOW_PERCENT = 48

    const val ATTRIBUTION = "WHO (Five) Well-Being Index (1998 version). © Psychiatric Research Unit, " +
        "WHO Collaborating Center for Mental Health, Frederiksborg General Hospital, Hillerød. Copyright now held by WHO."

    /** The design doc's cadence: monthly. */
    const val DAYS_BETWEEN = 28L

    fun percent(rawScore: Int): Int = rawScore.coerceIn(0, 25) * 4

    /** Poor wellbeing by the official rule: raw score below 13, or any single answer of 0 or 1. */
    fun indicatesPoorWellbeing(answers: List<Int>): Boolean = answers.sum() < LOW_RAW_BELOW || answers.any { it <= 1 }

    fun isDue(results: Map<LocalDate, Int>, today: LocalDate): Boolean {
        val last = results.keys.maxOrNull() ?: return true
        return ChronoUnit.DAYS.between(last, today) >= DAYS_BETWEEN
    }
}
