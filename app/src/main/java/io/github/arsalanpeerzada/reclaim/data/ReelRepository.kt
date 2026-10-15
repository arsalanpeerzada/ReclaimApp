package io.github.arsalanpeerzada.reclaim.data

import io.github.arsalanpeerzada.reclaim.counting.TrackedApp
import io.github.arsalanpeerzada.reclaim.domain.History
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ReelRepository(
    private val dao: ReclaimDao,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    fun today(): LocalDate = LocalDate.now(clock)

    /** Records one reel and returns today's total across apps. */
    suspend fun recordReel(app: TrackedApp, budget: Int): Int = dao.recordReel(today().toEpochDay(), app.id, budget)

    /** Applies a new budget from today onwards; earlier days keep the budget they had. */
    suspend fun updateGoals(dailyBudget: Int, typicalReelsPerDay: Int) {
        settings.updateGoals(dailyBudget, typicalReelsPerDay)
        dao.upsertBudget(DailyBudget(today().toEpochDay(), dailyBudget))
    }

    val history: Flow<History> = combine(dao.observeReels(), dao.observeBudgets()) { reels, budgets ->
        History(
            reels = reels
                .groupBy { LocalDate.ofEpochDay(it.epochDay) }
                .mapValues { (_, rows) ->
                    rows.mapNotNull { row -> TrackedApp.fromId(row.app)?.let { it to row.count } }.toMap()
                },
            budgets = budgets.associate { LocalDate.ofEpochDay(it.epochDay) to it.budget },
        )
    }
}
