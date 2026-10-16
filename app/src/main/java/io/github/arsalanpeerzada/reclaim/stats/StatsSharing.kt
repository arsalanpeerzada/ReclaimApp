package io.github.arsalanpeerzada.reclaim.stats

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.github.arsalanpeerzada.reclaim.AppContainer
import io.github.arsalanpeerzada.reclaim.ReclaimApp
import io.github.arsalanpeerzada.reclaim.domain.Levels
import io.github.arsalanpeerzada.reclaim.domain.Progress
import io.github.arsalanpeerzada.reclaim.domain.Streaks
import io.github.arsalanpeerzada.reclaim.domain.Who5
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Opt-in anonymous stats for the admin dashboard. With the user's yes, completed days are uploaded
 * as small summaries (see [StatsSummaries]) under an anonymous Firebase account: no name, email,
 * device id or content. Without a yes, or without a Firebase config in the build, nothing happens.
 */
class StatsSharing(private val context: Context, private val container: AppContainer) {
    /** Whether this build has a Firebase project (google-services.json) to share with. */
    val available: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun enable(includeWellbeing: Boolean) {
        container.settings.setSharing(stats = true, wellbeing = includeWellbeing)
        schedule()
        uploadSoon()
    }

    /** Keeps the daily upload scheduled while the user is sharing (called at app start). */
    suspend fun ensureScheduled() {
        if (available && container.settings.settings.first().shareStats) schedule()
    }

    /** Stops sharing and deletes everything this phone shared, including its anonymous account. */
    suspend fun stopAndDelete(): Unit = lock.withLock {
        container.settings.setSharing(stats = false, wellbeing = false)
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
        if (!available) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userDoc = FirebaseFirestore.getInstance().collection("users").document(user.uid)
        for (sub in listOf("days", "checkins")) {
            userDoc.collection(sub).get().await().documents.chunked(BATCH).forEach { chunk ->
                val batch = FirebaseFirestore.getInstance().batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
        userDoc.delete().await()
        user.delete().await()
    }

    /**
     * Uploads completed days not yet shared. True when done (or nothing to do), false to retry later.
     * One at a time: two uploads racing would each sign in and create two anonymous accounts.
     */
    suspend fun upload(): Boolean = lock.withLock { uploadLocked() }

    private suspend fun uploadLocked(): Boolean {
        val settings = container.settings.settings.first()
        if (!settings.shareStats || !available) return true
        val startDay = settings.startDay ?: return true

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: auth.signInAnonymously().await().user ?: return false
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(user.uid)

        val today = container.reels.today()
        val history = container.reels.history.first()
        val quests = container.quests.completed.first()
        val moods = container.wellbeing.moods.first()
        val who5 = container.wellbeing.who5.first()
        val pauses = container.dao.observePauses().first().associate { LocalDate.ofEpochDay(it.epochDay) to PauseCounts(it.shown, it.stopped) }

        val completed = Progress.days(history, today, startDay, settings.dailyBudget).dropLast(1)
        val pending = StatsSummaries.pending(completed, settings.lastUploadedDay)
        val streak = Streaks.compute(completed)

        val writes = mutableListOf<Pair<DocumentReference, Map<String, Any?>>>()
        writes += userDoc to mapOf(
            "startDay" to startDay.toString(),
            "typicalReelsPerDay" to settings.typicalReelsPerDay,
            "dailyBudget" to settings.dailyBudget,
            "level" to Levels.of(streak.daysWithinBudget).number,
            "streak" to streak.current,
            "graduatedOn" to settings.graduatedOn?.toString(),
            "sharesWellbeing" to settings.shareWellbeing,
            "appVersion" to appVersion(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        pending.forEach { day ->
            writes += userDoc.collection("days").document(day.date.toString()) to StatsSummaries.day(
                day = day,
                startDay = startDay,
                history = history,
                questsDone = quests[day.date]?.size ?: 0,
                pauses = pauses[day.date],
                mood = moods[day.date],
                includeWellbeing = settings.shareWellbeing,
            )
        }
        if (settings.shareWellbeing) {
            who5.forEach { (date, raw) ->
                writes += userDoc.collection("checkins").document(date.toString()) to
                    mapOf("date" to date.toString(), "score" to Who5.percent(raw))
            }
        }

        writes.chunked(BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, data) -> batch.set(ref, data, SetOptions.merge()) }
            batch.commit().await()
        }
        pending.lastOrNull()?.let { container.settings.markUploaded(it.date) }
        return true
    }

    private fun schedule() {
        val request = PeriodicWorkRequestBuilder<StatsUploadWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun uploadSoon() {
        val request = OneTimeWorkRequestBuilder<StatsUploadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
    }

    private fun appVersion(): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "unknown"

    private companion object {
        /** Shared by every upload and delete in the process. */
        val lock = Mutex()

        const val PERIODIC = "stats-upload"
        const val NOW = "stats-upload-now"

        /** Firestore allows 500 writes per batch; stay well under. */
        const val BATCH = 400
    }
}

class StatsUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as ReclaimApp).container
        return if (runCatching { container.stats.upload() }.getOrDefault(false)) Result.success() else Result.retry()
    }
}
