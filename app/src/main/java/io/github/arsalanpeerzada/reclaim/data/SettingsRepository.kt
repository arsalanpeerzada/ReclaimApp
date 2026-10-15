package io.github.arsalanpeerzada.reclaim.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val onboarded: Boolean,
    /** The user's own daily limit. */
    val dailyBudget: Int,
    /** The user's estimate of a normal day before Reclaim: the baseline for reclaimed time. */
    val typicalReelsPerDay: Int,
    /** The day counting started; streaks and reclaimed time are measured from here. */
    val startDay: LocalDate?,
    /** Show the mindful pause at 80% and 100% of the budget. On by default; the user's choice. */
    val pauseEnabled: Boolean,
    /** Show the live counter pill at the top of Instagram and TikTok. On by default. */
    val counterBubbleEnabled: Boolean,
    /** The Monday of the last week the user reflected on. */
    val lastReflectedWeek: LocalDate?,
    /** When the user graduated: Reclaim did its job. */
    val graduatedOn: LocalDate?,
    /** When the user last said "not yet" to graduating; asked again a month later. */
    val graduationDismissedOn: LocalDate?,
    /** Opted in to sharing anonymous daily summaries. Off unless the user says yes. */
    val shareStats: Boolean,
    /** Within that, also share mood tags and WHO-5 scores (health data: its own explicit yes). */
    val shareWellbeing: Boolean,
    /** The last completed day already uploaded. */
    val lastUploadedDay: LocalDate?,
    /** The highest level already celebrated, so each level-up gets its moment once. */
    val celebratedLevel: Int,
) {
    companion object {
        const val DEFAULT_TYPICAL = 200
        const val DEFAULT_BUDGET = 100
    }
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            onboarded = prefs[ONBOARDED] ?: false,
            dailyBudget = prefs[DAILY_BUDGET] ?: Settings.DEFAULT_BUDGET,
            typicalReelsPerDay = prefs[TYPICAL] ?: Settings.DEFAULT_TYPICAL,
            startDay = prefs[START_DAY]?.let(LocalDate::ofEpochDay),
            pauseEnabled = prefs[PAUSE_ENABLED] ?: true,
            counterBubbleEnabled = prefs[COUNTER_BUBBLE] ?: true,
            lastReflectedWeek = prefs[LAST_REFLECTED_WEEK]?.let(LocalDate::ofEpochDay),
            graduatedOn = prefs[GRADUATED_ON]?.let(LocalDate::ofEpochDay),
            graduationDismissedOn = prefs[GRADUATION_DISMISSED_ON]?.let(LocalDate::ofEpochDay),
            shareStats = prefs[SHARE_STATS] ?: false,
            shareWellbeing = prefs[SHARE_WELLBEING] ?: false,
            lastUploadedDay = prefs[LAST_UPLOADED_DAY]?.let(LocalDate::ofEpochDay),
            celebratedLevel = prefs[CELEBRATED_LEVEL] ?: 1,
        )
    }

    suspend fun completeOnboarding(dailyBudget: Int, typicalReelsPerDay: Int, today: LocalDate) {
        dataStore.edit { prefs ->
            prefs[DAILY_BUDGET] = dailyBudget
            prefs[TYPICAL] = typicalReelsPerDay
            if (prefs[START_DAY] == null) prefs[START_DAY] = today.toEpochDay()
            prefs[ONBOARDED] = true
        }
    }

    suspend fun updateGoals(dailyBudget: Int, typicalReelsPerDay: Int) {
        dataStore.edit { prefs ->
            prefs[DAILY_BUDGET] = dailyBudget
            prefs[TYPICAL] = typicalReelsPerDay
        }
    }

    suspend fun setPauseEnabled(enabled: Boolean) {
        dataStore.edit { it[PAUSE_ENABLED] = enabled }
    }

    suspend fun setCounterBubbleEnabled(enabled: Boolean) {
        dataStore.edit { it[COUNTER_BUBBLE] = enabled }
    }

    suspend fun markReflected(weekStart: LocalDate) {
        dataStore.edit { it[LAST_REFLECTED_WEEK] = weekStart.toEpochDay() }
    }

    suspend fun graduate(day: LocalDate) {
        dataStore.edit { it[GRADUATED_ON] = day.toEpochDay() }
    }

    suspend fun dismissGraduation(day: LocalDate) {
        dataStore.edit { it[GRADUATION_DISMISSED_ON] = day.toEpochDay() }
    }

    suspend fun setSharing(stats: Boolean, wellbeing: Boolean) {
        dataStore.edit {
            it[SHARE_STATS] = stats
            it[SHARE_WELLBEING] = stats && wellbeing
            // Stopping and starting again re-sends history from the beginning.
            if (!stats) it.remove(LAST_UPLOADED_DAY)
        }
    }

    suspend fun markCelebrated(level: Int) {
        dataStore.edit { it[CELEBRATED_LEVEL] = level }
    }

    suspend fun markUploaded(day: LocalDate) {
        dataStore.edit { it[LAST_UPLOADED_DAY] = day.toEpochDay() }
    }

    /** Forgets every setting, back to a fresh install. */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val DAILY_BUDGET = intPreferencesKey("daily_budget")
        val TYPICAL = intPreferencesKey("typical_reels_per_day")
        val START_DAY = longPreferencesKey("start_epoch_day")
        val PAUSE_ENABLED = booleanPreferencesKey("pause_enabled")
        val LAST_REFLECTED_WEEK = longPreferencesKey("last_reflected_week")
        val COUNTER_BUBBLE = booleanPreferencesKey("counter_bubble")
        val GRADUATED_ON = longPreferencesKey("graduated_on")
        val GRADUATION_DISMISSED_ON = longPreferencesKey("graduation_dismissed_on")
        val SHARE_STATS = booleanPreferencesKey("share_stats")
        val SHARE_WELLBEING = booleanPreferencesKey("share_wellbeing")
        val LAST_UPLOADED_DAY = longPreferencesKey("last_uploaded_day")
        val CELEBRATED_LEVEL = intPreferencesKey("celebrated_level")
    }
}
