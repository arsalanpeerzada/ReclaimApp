package io.github.arsalanpeerzada.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrendsTest {
    private val start = LocalDate.of(2026, 9, 1)

    private fun days(reels: List<Int>, budget: Int = 100) =
        reels.mapIndexed { i, r -> DayOutcome(start.plusDays(i.toLong()), r, budget) }

    @Test
    fun `no trend before a week is tracked`() {
        assertNull(Trends.lastMonth(days(List(6) { 50 })))
    }

    @Test
    fun `rolling month against the first week`() {
        // First week 200 a day, then 37 days at 50.
        val trend = Trends.lastMonth(days(List(7) { 200 } + List(37) { 50 }))!!
        assertEquals(30, trend.daysTracked)
        assertEquals(100, trend.percentWithinBudget)
        assertEquals(50, trend.averagePerDay)
        assertEquals(200, trend.startingAverage)
        assertEquals(-75, trend.changeFromStartPercent)
    }

    @Test
    fun `graduation needs a full, steady month well below the usual day`() {
        val steady = days(List(30) { 40 })
        assertTrue(Trends.canGraduate(steady, typicalReelsPerDay = 200))
        // Too high compared with the usual day.
        assertFalse(Trends.canGraduate(steady, typicalReelsPerDay = 60))
        // Not a full month yet.
        assertFalse(Trends.canGraduate(days(List(20) { 40 }), typicalReelsPerDay = 200))
        // Four days over budget in thirty: 87%.
        assertFalse(Trends.canGraduate(days(List(26) { 40 } + List(4) { 150 }), typicalReelsPerDay = 400))
    }
}
