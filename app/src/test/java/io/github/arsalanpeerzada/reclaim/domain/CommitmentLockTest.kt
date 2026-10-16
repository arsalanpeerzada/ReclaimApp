package io.github.arsalanpeerzada.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CommitmentLockTest {
    private val today = LocalDate.of(2026, 10, 16)

    @Test
    fun `never locks unless switched on`() {
        assertNull(CommitmentLock.state(enabled = false, reelsToday = 500, budget = 10, unlockedOn = null, today = today))
    }

    @Test
    fun `locks once the budget is used`() {
        assertNull(CommitmentLock.state(true, reelsToday = 9, budget = 10, unlockedOn = null, today = today))
        val locked = CommitmentLock.state(true, reelsToday = 10, budget = 10, unlockedOn = null, today = today)!!
        assertTrue(locked.canUnlock)
    }

    @Test
    fun `the emergency unlock gives five more, once a day`() {
        assertNull(CommitmentLock.state(true, reelsToday = 14, budget = 10, unlockedOn = today, today = today))
        val again = CommitmentLock.state(true, reelsToday = 15, budget = 10, unlockedOn = today, today = today)!!
        assertFalse(again.canUnlock)
        // Yesterday's unlock doesn't carry over.
        assertEquals(0, CommitmentLock.extraToday(today.minusDays(1), today))
        assertTrue(CommitmentLock.state(true, 10, 10, today.minusDays(1), today)!!.canUnlock)
    }
}
