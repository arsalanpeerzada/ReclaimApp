package io.github.arsalanpeerzada.reclaim

import android.app.Application
import android.content.Context
import androidx.room.Room
import io.github.arsalanpeerzada.reclaim.counting.CountingDiagnostics
import io.github.arsalanpeerzada.reclaim.counting.TrackedApp
import io.github.arsalanpeerzada.reclaim.data.QuestRepository
import io.github.arsalanpeerzada.reclaim.data.ReclaimDatabase
import io.github.arsalanpeerzada.reclaim.data.ReelRepository
import io.github.arsalanpeerzada.reclaim.data.SettingsRepository
import io.github.arsalanpeerzada.reclaim.data.WellbeingRepository
import io.github.arsalanpeerzada.reclaim.data.settingsDataStore
import io.github.arsalanpeerzada.reclaim.domain.PauseRequest
import io.github.arsalanpeerzada.reclaim.domain.PauseStage
import io.github.arsalanpeerzada.reclaim.domain.PauseTracker
import io.github.arsalanpeerzada.reclaim.stats.StatsSharing
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Hand-wired dependencies, shared by the activity and the counting service (same process). */
class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clock = Clock.systemDefaultZone()

    private val database = Room.databaseBuilder(context, ReclaimDatabase::class.java, "reclaim.db")
        .addMigrations(ReclaimDatabase.MIGRATION_1_2, ReclaimDatabase.MIGRATION_2_3, ReclaimDatabase.MIGRATION_3_4)
        .build()

    val settings = SettingsRepository(context.settingsDataStore)
    val reels = ReelRepository(database.dao(), settings, clock)
    val quests = QuestRepository(database.dao(), clock)
    val wellbeing = WellbeingRepository(database.dao(), clock)
    val diagnostics = CountingDiagnostics()
    val dao = database.dao()
    val stats = StatsSharing(context.applicationContext, this)

    private val pauseTracker = PauseTracker()
    private val pauseLock = Mutex()
    private val _pauseRequests = MutableSharedFlow<PauseRequest>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** When the counting service should show the mindful pause. */
    val pauseRequests: SharedFlow<PauseRequest> = _pauseRequests.asSharedFlow()

    /**
     * Deletes everything Reclaim has stored: counts, budgets, quests, moods, check-ins and settings.
     * The app is back at onboarding afterwards. There is no copy anywhere else to delete.
     */
    suspend fun deleteAllData() {
        // Anything already shared goes too, while we still know whose it was.
        if (settings.settings.first().shareStats) runCatching { stats.stopAndDelete() }
        withContext(Dispatchers.IO) { database.clearAllTables() }
        settings.clearAll()
        diagnostics.clear()
    }

    /** Background jobs that should run whenever the app process starts. */
    fun start() {
        applicationScope.launch { runCatching { stats.ensureScheduled() } }
    }

    /** Today's one emergency unlock of the commitment lock. */
    fun useEmergencyUnlock() {
        applicationScope.launch { settings.useEmergencyUnlock(reels.today()) }
    }

    /** The mindful pause showed; [stopped] when the user chose "I'm done for now". */
    fun recordPause(stopped: Boolean) {
        applicationScope.launch { dao.recordPause(reels.today().toEpochDay(), shown = if (stopped) 0 else 1, stopped = if (stopped) 1 else 0) }
    }

    fun recordReel(app: TrackedApp) {
        applicationScope.launch {
            val current = settings.settings.first()
            val total = reels.recordReel(app, current.dailyBudget)
            if (!current.pauseEnabled) return@launch
            // Reels can arrive back to back; decide one at a time so a stage is never shown twice.
            val request = pauseLock.withLock { pauseTracker.onReel(reels.today(), total, current.dailyBudget) }
            // With the commitment lock on, the lock replaces the "budget reached" pause.
            if (request?.stage == PauseStage.BudgetReached && current.commitmentLock) return@launch
            request?.let(_pauseRequests::tryEmit)
        }
    }
}

class ReclaimApp : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.start()
    }
}
