package io.github.arsalanpeerzada.reclaim.domain

import java.time.LocalDate

/** What the lock screen needs to know. */
data class LockState(
    val reels: Int,
    val budget: Int,
    /** Whether today's one emergency unlock is still available. */
    val canUnlock: Boolean,
)

/**
 * The opt-in commitment device from the design doc: hard blocks only if the user asks for them.
 * Once today's budget (plus any emergency extra) is used, Instagram and TikTok are locked until
 * midnight. One emergency unlock a day, after a wait, buys a few more reels: firm, but never a trap
 * that makes people uninstall.
 */
object CommitmentLock {
    const val EXTRA_REELS = 5
    const val WAIT_SECONDS = 60

    /** Extra reels granted today, given the day the last unlock was used. */
    fun extraToday(unlockedOn: LocalDate?, today: LocalDate): Int = if (unlockedOn == today) EXTRA_REELS else 0

    /** Null when not locked (switched off, or budget not yet used). */
    fun state(enabled: Boolean, reelsToday: Int, budget: Int, unlockedOn: LocalDate?, today: LocalDate): LockState? {
        if (!enabled || budget <= 0) return null
        val allowance = budget + extraToday(unlockedOn, today)
        if (reelsToday < allowance) return null
        return LockState(reels = reelsToday, budget = budget, canUnlock = unlockedOn != today)
    }
}
