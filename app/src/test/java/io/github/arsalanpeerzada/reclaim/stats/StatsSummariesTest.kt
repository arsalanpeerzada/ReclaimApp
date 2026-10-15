package io.github.arsalanpeerzada.reclaim.stats

import io.github.arsalanpeerzada.reclaim.counting.TrackedApp
import io.github.arsalanpeerzada.reclaim.domain.DayOutcome
import io.github.arsalanpeerzada.reclaim.domain.History
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatsSummariesTest {
    private val start = LocalDate.of(2026, 10, 1)
    private val day = DayOutcome(start.plusDays(2), reels = 30, budget = 50)
    private val history = History(reels = mapOf(day.date to mapOf(TrackedApp.INSTAGRAM to 18, TrackedApp.TIKTOK to 12)), budgets = emptyMap())

    @Test
    fun `a shared day holds only the agreed fields`() {
        val summary = StatsSummaries.day(day, start, history, questsDone = 2, pauses = PauseCounts(1, 1), mood = 4, includeWellbeing = false)
        // Must match the field list allowed by firebase/firestore.rules.
        assertEquals(
            setOf("date", "dayIndex", "reels", "budget", "within", "instagram", "tiktok", "quests", "pausesShown", "pausesStopped"),
            summary.keys,
        )
        assertEquals("2026-10-03", summary["date"])
        assertEquals(2, summary["dayIndex"])
        assertEquals(true, summary["within"])
        assertEquals(18, summary["instagram"])
    }

    @Test
    fun `mood is shared only with the separate wellbeing consent`() {
        assertFalse("mood" in StatsSummaries.day(day, start, history, 0, null, mood = 4, includeWellbeing = false))
        assertTrue("mood" in StatsSummaries.day(day, start, history, 0, null, mood = 4, includeWellbeing = true))
    }

    @Test
    fun `only days after the last upload are pending`() {
        val days = (0..4).map { DayOutcome(start.plusDays(it.toLong()), 10, 50) }
        assertEquals(5, StatsSummaries.pending(days, null).size)
        assertEquals(listOf(start.plusDays(3), start.plusDays(4)), StatsSummaries.pending(days, start.plusDays(2)).map { it.date })
    }
}
