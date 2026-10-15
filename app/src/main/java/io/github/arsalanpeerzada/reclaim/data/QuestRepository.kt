package io.github.arsalanpeerzada.reclaim.data

import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuestRepository(private val dao: ReclaimDao, private val clock: Clock) {
    /** Completed quest ids per day. */
    val completed: Flow<Map<LocalDate, Set<String>>> = dao.observeQuestsDone().map { rows ->
        rows.groupBy({ LocalDate.ofEpochDay(it.epochDay) }, { it.questId }).mapValues { (_, ids) -> ids.toSet() }
    }

    /** Ticks a quest off today, or unticks it: the user's word is enough. */
    suspend fun setDone(questId: String, done: Boolean) {
        val day = LocalDate.now(clock).toEpochDay()
        if (done) dao.insertQuestDone(QuestDone(day, questId)) else dao.deleteQuestDone(day, questId)
    }
}
