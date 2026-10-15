package io.github.arsalanpeerzada.reclaim.domain

/**
 * Identity levels from the design doc: people keep behaviours that match who they think they are.
 * Earned by days within budget in total, so a bad day never takes a level away.
 */
data class Level(
    val number: Int,
    val name: String,
    /** Days within budget still needed for the next level; null at the top. */
    val daysToNext: Int?,
)

object Levels {
    private data class Tier(val name: String, val fromDays: Int)

    // Thresholds are a first guess, like the design doc's other defaults.
    private val tiers = listOf(Tier("Scroller", 0), Tier("Intentional", 7), Tier("Master", 30))

    fun of(daysWithinBudget: Int): Level {
        val index = tiers.indexOfLast { daysWithinBudget >= it.fromDays }
        val next = tiers.getOrNull(index + 1)
        return Level(number = index + 1, name = tiers[index].name, daysToNext = next?.let { it.fromDays - daysWithinBudget })
    }
}
