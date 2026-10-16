package io.github.arsalanpeerzada.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PhaseThreeTest {
    // Monday 5 October 2026.
    private val monday = LocalDate.of(2026, 10, 5)

    private fun week(start: LocalDate, reels: List<Int>, budget: Int = 100) =
        reels.mapIndexed { i, r -> DayOutcome(start.plusDays(i.toLong()), r, budget) }

    @Test
    fun `weeks start on Monday`() {
        assertEquals(monday, Weeks.startOf(LocalDate.of(2026, 10, 11)))
        assertEquals(monday, Weeks.lastCompleted(LocalDate.of(2026, 10, 14)))
    }

    @Test
    fun `reflection sums the week and compares with the week before`() {
        val before = week(monday.minusWeeks(1), List(7) { 100 })
        val this_ = week(monday, listOf(50, 60, 70, 80, 90, 40, 30))
        val r = Reflections.build(before + this_, emptyMap(), monday, currentBudget = 100)!!
        assertEquals(420, r.totalReels)
        assertEquals(60, r.averagePerDay)
        assertEquals(7, r.daysWithinBudget)
        assertEquals(-40, r.changePercent)
        assertEquals(monday.plusDays(6), r.bestDay.date)
        // Every day within budget: offer 10% less.
        assertEquals(90, r.suggestedBudget)
    }

    @Test
    fun `no reflection for a week with no tracked days`() {
        assertNull(Reflections.build(week(monday.minusWeeks(2), listOf(10)), emptyMap(), monday, 100))
    }

    @Test
    fun `budget suggestions stay doable and need enough days`() {
        assertEquals(110, Reflections.suggestBudget(within = 2, tracked = 7, current = 100))
        assertNull(Reflections.suggestBudget(within = 4, tracked = 7, current = 100))
        assertNull(Reflections.suggestBudget(within = 3, tracked = 3, current = 100))
        assertNull(Reflections.suggestBudget(within = 7, tracked = 7, current = 5))
    }

    @Test
    fun `mood insight compares low-mood days with the rest`() {
        val days = week(monday, listOf(200, 200, 100, 100, 100, 100, 100))
        val moods = days.mapIndexed { i, d -> d.date to if (i < 2) 1 else 4 }.toMap()
        assertTrue(Reflections.moodInsight(days, moods).startsWith("You scrolled 2x as much on low-mood days"))
        assertTrue(Reflections.moodInsight(days, emptyMap()).startsWith("Tag your mood"))
    }

    @Test
    fun `WHO-5 poor wellbeing follows the official rule`() {
        // Raw 12 is below 13.
        assertTrue(Who5.indicatesPoorWellbeing(listOf(3, 3, 2, 2, 2)))
        // Raw 13, but one answer of 1: still poor.
        assertTrue(Who5.indicatesPoorWellbeing(listOf(4, 3, 3, 2, 1)))
        // Raw 13 with every answer 2 or more: not poor.
        assertFalse(Who5.indicatesPoorWellbeing(listOf(3, 3, 3, 2, 2)))
    }

    @Test
    fun `WHO-5 scores as a percentage and is due monthly`() {
        assertEquals(100, Who5.percent(25))
        assertEquals(48, Who5.percent(12))
        assertTrue(Who5.isDue(emptyMap(), monday))
        assertFalse(Who5.isDue(mapOf(monday to 15), monday.plusDays(27)))
        assertTrue(Who5.isDue(mapOf(monday to 15), monday.plusDays(28)))
    }
}
