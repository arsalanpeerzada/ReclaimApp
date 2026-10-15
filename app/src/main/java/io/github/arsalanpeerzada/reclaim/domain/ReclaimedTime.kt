package io.github.arsalanpeerzada.reclaim.domain

data class Reclaimed(
    /** Reels below the user's typical day, summed over completed days. */
    val reelsAvoided: Int,
    val minutes: Int,
    /** Something the time could buy, such as "1 book chapter"; null below the smallest one. */
    val equivalent: String?,
)

/** Turns reels not watched into time, and time into something that means more than a number. */
object ReclaimedTime {
    /** Design-doc assumption, still to be checked against real usage. */
    const val SECONDS_PER_REEL = 5

    private data class Equivalent(val minutes: Int, val singular: String, val plural: String)

    // Largest first.
    private val equivalents = listOf(
        Equivalent(120, "film", "films"),
        Equivalent(25, "book chapter", "book chapters"),
        Equivalent(10, "10-minute walk", "10-minute walks"),
    )

    fun compute(completedDays: List<DayOutcome>, typicalReelsPerDay: Int): Reclaimed {
        val avoided = completedDays.sumOf { (typicalReelsPerDay - it.reels).coerceAtLeast(0) }
        val minutes = avoided * SECONDS_PER_REEL / 60
        return Reclaimed(reelsAvoided = avoided, minutes = minutes, equivalent = equivalentOf(minutes))
    }

    fun equivalentOf(minutes: Int): String? {
        val match = equivalents.firstOrNull { minutes >= it.minutes } ?: return null
        val count = minutes / match.minutes
        return "$count ${if (count == 1) match.singular else match.plural}"
    }
}
