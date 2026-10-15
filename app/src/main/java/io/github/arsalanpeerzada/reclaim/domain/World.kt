package io.github.arsalanpeerzada.reclaim.domain

import java.time.LocalDate

enum class PlantKind { Tulip, Daisy, Mushroom, Sunflower, Cactus, Bush, Tree, QuestStar, RareBloom }

enum class PlantSource { BudgetDay, Quest, Surprise }

data class Plant(val kind: PlantKind, val source: PlantSource, val planted: LocalDate, val wilted: Boolean)

enum class Seedling {
    /** Today is within budget so far: something blooms tonight if it stays that way. */
    Growing,

    /** Today is over budget: nothing new tonight, and the newest plant droops. */
    Resting,
}

data class World(
    /** Oldest first. */
    val plants: List<Plant>,
    val today: Seedling,
) {
    val wilting: Int get() = plants.count { it.wilted }
}

/**
 * Pip's garden, rebuilt from history every time so it can never drift. Each day within budget plants
 * something; every seventh is a tree. A day over budget makes the newest budget plant droop (it is never
 * removed) until the next good day perks it up: "wilts gently", as the design doc puts it. Quest plants
 * never wilt; they are earned offline.
 */
object WorldBuilder {
    private val dayKinds = listOf(PlantKind.Tulip, PlantKind.Daisy, PlantKind.Mushroom, PlantKind.Sunflower, PlantKind.Cactus, PlantKind.Bush)
    const val TREE_EVERY = 7

    /**
     * [completedDays] in date order, not including [today]; [quests] holds each day's completed
     * quest ids, today's included.
     */
    fun build(completedDays: List<DayOutcome>, today: DayOutcome, quests: Map<LocalDate, Set<String>>): World {
        val plants = mutableListOf<Plant>()
        var budgetPlants = 0

        fun droopNewest() {
            val index = plants.indexOfLast { it.source == PlantSource.BudgetDay }
            if (index >= 0) plants[index] = plants[index].copy(wilted = true)
        }

        fun plantQuests(date: LocalDate) {
            quests[date].orEmpty().sorted().forEach { id ->
                val surprise = Quests.isSurprise(date, id)
                plants += Plant(
                    kind = if (surprise) PlantKind.RareBloom else PlantKind.QuestStar,
                    source = if (surprise) PlantSource.Surprise else PlantSource.Quest,
                    planted = date,
                    wilted = false,
                )
            }
        }

        for (day in completedDays) {
            plantQuests(day.date)
            if (day.withinBudget) {
                plants.replaceAll { it.copy(wilted = false) }
                budgetPlants++
                val kind = if (budgetPlants % TREE_EVERY == 0) PlantKind.Tree else dayKinds[(budgetPlants - 1) % dayKinds.size]
                plants += Plant(kind, PlantSource.BudgetDay, day.date, wilted = false)
            } else {
                droopNewest()
            }
        }
        plantQuests(today.date)
        // Today is live: going over droops the newest plant straight away, gently.
        if (!today.withinBudget) droopNewest()
        return World(plants, if (today.withinBudget) Seedling.Growing else Seedling.Resting)
    }
}
