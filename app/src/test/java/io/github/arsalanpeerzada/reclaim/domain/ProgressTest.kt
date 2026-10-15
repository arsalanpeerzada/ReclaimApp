package io.github.arsalanpeerzada.reclaim.domain

import io.github.arsalanpeerzada.reclaim.counting.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ProgressTest {
    private val start = LocalDate.of(2026, 10, 10)
    private val today = start.plusDays(3)

    @Test
    fun `reclaimed time matches the design doc example`() {
        // 300 reels avoided at 5 seconds each = 25 minutes = one book chapter.
        val day = DayOutcome(start, reels = 0, budget = 100)
        val reclaimed = ReclaimedTime.compute(listOf(day), typicalReelsPerDay = 300)
        assertEquals(300, reclaimed.reelsAvoided)
        assertEquals(25, reclaimed.minutes)
        assertEquals("1 book chapter", reclaimed.equivalent)
    }

    @Test
    fun `days above the usual amount reclaim nothing rather than a negative`() {
        val day = DayOutcome(start, reels = 500, budget = 100)
        assertEquals(0, ReclaimedTime.compute(listOf(day), typicalReelsPerDay = 200).reelsAvoided)
        assertNull(ReclaimedTime.equivalentOf(9))
        assertEquals("2 10-minute walks", ReclaimedTime.equivalentOf(24))
        assertEquals("1 film", ReclaimedTime.equivalentOf(130))
    }

    @Test
    fun `overview fills empty days, keeps past budgets and excludes today from the streak`() {
        val history = History(
            reels = mapOf(
                start to mapOf(TrackedApp.INSTAGRAM to 30, TrackedApp.TIKTOK to 20),
                start.plusDays(1) to mapOf(TrackedApp.TIKTOK to 80),
                today to mapOf(TrackedApp.INSTAGRAM to 90),
            ),
            // Day two had a budget of 100; the budget is now 50.
            budgets = mapOf(start to 60, start.plusDays(1) to 100),
        )
        val overview = Progress.overview(history, today, start, currentBudget = 50, typicalReelsPerDay = 200)

        assertEquals(DayOutcome(today, 90, 50), overview.today)
        assertEquals(1.8f, overview.todayFraction, 0.001f)
        assertEquals(mapOf(TrackedApp.INSTAGRAM to 90), overview.todayByApp)
        // Day one 50/60, day two 80/100, day three 0/50: all within budget; today does not count yet.
        assertEquals(3, overview.streak.current)
        assertEquals(4, overview.lastSevenDays.size)
        assertEquals((150 + 120 + 200), overview.reclaimed.reelsAvoided)
    }

    @Test
    fun `the first day shows a single day with no streak`() {
        val overview = Progress.overview(History(emptyMap(), emptyMap()), start, start, currentBudget = 50, typicalReelsPerDay = 200)
        assertEquals(1, overview.lastSevenDays.size)
        assertEquals(0, overview.streak.current)
        assertEquals(0, overview.reclaimed.minutes)
    }
}
