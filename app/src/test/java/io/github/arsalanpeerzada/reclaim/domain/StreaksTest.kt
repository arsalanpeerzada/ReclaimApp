package io.github.arsalanpeerzada.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreaksTest {
    private val start = LocalDate.of(2026, 10, 1)

    /** One day per character: 'u' under budget, 'o' over. */
    private fun days(pattern: String) = pattern.mapIndexed { i, c ->
        DayOutcome(start.plusDays(i.toLong()), reels = if (c == 'u') 10 else 200, budget = 100)
    }

    @Test
    fun `no history means no streak and the starting freeze`() {
        val state = Streaks.compute(emptyList())
        assertEquals(0, state.current)
        assertEquals(Streaks.STARTING_FREEZES, state.freezesLeft)
    }

    @Test
    fun `consecutive days under budget build the streak`() {
        assertEquals(3, Streaks.compute(days("uuu")).current)
    }

    @Test
    fun `a freeze bridges one day over budget`() {
        val state = Streaks.compute(days("uuouu"))
        assertEquals(4, state.current)
        assertEquals(0, state.freezesLeft)
        assertEquals(setOf(start.plusDays(2)), state.frozenDays)
    }

    @Test
    fun `without freezes a day over budget resets the streak but keeps the best`() {
        val state = Streaks.compute(days("uuoouu"))
        assertEquals(2, state.current)
        assertEquals(2, state.best)
    }

    @Test
    fun `seven days under budget earn a freeze, up to the maximum`() {
        assertEquals(2, Streaks.compute(days("uuuuuuu")).freezesLeft)
        assertEquals(Streaks.MAX_FREEZES, Streaks.compute(days("u".repeat(21))).freezesLeft)
    }

    @Test
    fun `a day exactly on budget counts as within it`() {
        val day = DayOutcome(start, reels = 100, budget = 100)
        assertEquals(1, Streaks.compute(listOf(day)).current)
    }
}
