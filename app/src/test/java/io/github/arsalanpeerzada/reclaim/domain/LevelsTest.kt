package io.github.arsalanpeerzada.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LevelsTest {
    @Test
    fun `levels rise with days within budget`() {
        assertEquals(Level(1, "Scroller", 7), Levels.of(0))
        assertEquals(Level(1, "Scroller", 1), Levels.of(6))
        assertEquals(Level(2, "Intentional", 23), Levels.of(7))
        assertEquals(Level(3, "Master", null), Levels.of(30))
        assertNull(Levels.of(400).daysToNext)
    }

    @Test
    fun `days over budget never cost a level`() {
        val start = LocalDate.of(2026, 10, 1)
        // Seven good days, then a run of bad ones that also breaks the streak.
        val days = (0 until 12).map { i -> DayOutcome(start.plusDays(i.toLong()), reels = if (i < 7) 10 else 200, budget = 100) }
        val streak = Streaks.compute(days)
        assertEquals(0, streak.current)
        assertEquals(7, streak.daysWithinBudget)
        assertEquals("Intentional", Levels.of(streak.daysWithinBudget).name)
    }

    @Test
    fun `counts down to the next freeze`() {
        val start = LocalDate.of(2026, 10, 1)
        val days = (0 until 3).map { i -> DayOutcome(start.plusDays(i.toLong()), reels = 10, budget = 100) }
        assertEquals(4, Streaks.compute(days).daysToNextFreeze)
    }
}
