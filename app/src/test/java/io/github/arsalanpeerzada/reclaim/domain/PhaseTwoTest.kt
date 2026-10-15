package io.github.arsalanpeerzada.reclaim.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhaseTwoTest {
    private val start = LocalDate.of(2026, 10, 1)

    /** One day per character: 'u' under budget, 'o' over. */
    private fun days(pattern: String) = pattern.mapIndexed { i, c ->
        DayOutcome(start.plusDays(i.toLong()), reels = if (c == 'u') 10 else 200, budget = 100)
    }

    private fun todayAfter(pattern: String, over: Boolean = false) =
        DayOutcome(start.plusDays(pattern.length.toLong()), reels = if (over) 200 else 10, budget = 100)

    @Test
    fun `each day within budget plants something and every seventh is a tree`() {
        val world = WorldBuilder.build(days("uuuuuuu"), todayAfter("uuuuuuu"), emptyMap())
        assertEquals(7, world.plants.size)
        assertEquals(PlantKind.Tree, world.plants.last().kind)
        assertEquals(Seedling.Growing, world.today)
    }

    @Test
    fun `a day over droops the newest plant until the next good day`() {
        val drooping = WorldBuilder.build(days("uuo"), todayAfter("uuo"), emptyMap())
        assertEquals(2, drooping.plants.size)
        assertTrue(drooping.plants.last().wilted)
        assertFalse(drooping.plants.first().wilted)

        val recovered = WorldBuilder.build(days("uuou"), todayAfter("uuou"), emptyMap())
        assertEquals(3, recovered.plants.size)
        assertEquals(0, recovered.wilting)
    }

    @Test
    fun `going over today droops straight away and rests tonight's plot`() {
        val world = WorldBuilder.build(days("uu"), todayAfter("uu", over = true), emptyMap())
        assertEquals(Seedling.Resting, world.today)
        assertEquals(1, world.wilting)
    }

    @Test
    fun `quests add plants that never droop`() {
        val day = start.plusDays(1)
        val world = WorldBuilder.build(days("uo"), todayAfter("uo"), mapOf(day to setOf("walk")))
        val questPlant = world.plants.single { it.source != PlantSource.BudgetDay }
        assertFalse(questPlant.wilted)
        assertEquals(Quests.isSurprise(day, "walk"), questPlant.kind == PlantKind.RareBloom)
    }

    @Test
    fun `three different quests a day, stable within the day`() {
        val quests = Quests.forDay(start)
        assertEquals(Quests.PER_DAY, quests.map { it.id }.toSet().size)
        assertEquals(quests, Quests.forDay(start))
    }

    @Test
    fun `the pause shows once at 80 percent and once at the budget, per day`() {
        val tracker = PauseTracker()
        assertNull(tracker.onReel(start, 79, 100))
        assertEquals(PauseStage.Nudge, tracker.onReel(start, 80, 100)?.stage)
        assertNull(tracker.onReel(start, 81, 100))
        assertEquals(PauseStage.BudgetReached, tracker.onReel(start, 100, 100)?.stage)
        assertNull(tracker.onReel(start, 140, 100))
        // A new day starts afresh.
        assertEquals(PauseStage.Nudge, tracker.onReel(start.plusDays(1), 85, 100)?.stage)
    }

    @Test
    fun `jumping straight past the budget shows only the budget pause`() {
        val tracker = PauseTracker()
        assertEquals(PauseStage.BudgetReached, tracker.onReel(start, 12, 10)?.stage)
        assertNull(tracker.onReel(start, 13, 10))
    }
}
