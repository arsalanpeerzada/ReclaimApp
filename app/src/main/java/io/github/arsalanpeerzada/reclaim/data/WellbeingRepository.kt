package io.github.arsalanpeerzada.reclaim.data

import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Mood tags and WHO-5 check-ins. Like everything else, these never leave the phone. */
class WellbeingRepository(private val dao: ReclaimDao, private val clock: Clock) {
    val moods: Flow<Map<LocalDate, Int>> = dao.observeMoods().map { rows -> rows.associate { LocalDate.ofEpochDay(it.epochDay) to it.mood } }

    /** Raw WHO-5 totals (0 to 25) by day. */
    val who5: Flow<Map<LocalDate, Int>> = dao.observeWho5().map { rows -> rows.associate { LocalDate.ofEpochDay(it.epochDay) to it.rawScore } }

    suspend fun setMoodToday(mood: Int) = dao.upsertMood(DayMood(LocalDate.now(clock).toEpochDay(), mood.coerceIn(1, 5)))

    suspend fun saveWho5(rawScore: Int) = dao.upsertWho5(Who5Result(LocalDate.now(clock).toEpochDay(), rawScore.coerceIn(0, 25)))
}
