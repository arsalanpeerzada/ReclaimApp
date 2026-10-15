package io.github.arsalanpeerzada.reclaim.counting

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.content.ContextCompat
import io.github.arsalanpeerzada.reclaim.AppContainer
import io.github.arsalanpeerzada.reclaim.ReclaimApp
import io.github.arsalanpeerzada.reclaim.ui.bubble.BubbleState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Counts reels on the phone and shows the live counter and the mindful pause.
 *
 * What it reads, deliberately little: for every app, only the package name of the app coming to the
 * front, so the counter can appear in Instagram and TikTok and disappear elsewhere. For Instagram and
 * TikTok only, scroll positions, sizes and view ids. Never text or content, in any app.
 */
class ReelCountingService : AccessibilityService() {
    private val detector = ReelDetector()
    private var container: AppContainer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var pause: MindfulPauseOverlay? = null
    private var bubble: CounterBubbleOverlay? = null

    /** Whether Instagram or TikTok is the app in front. */
    private var trackedAppInFront = false

    private val debuggable by lazy { (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }

    override fun onServiceConnected() {
        val container = (application as ReclaimApp).container
        this.container = container

        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        val pause = MindfulPauseOverlay(this, onOutcome = container::recordPause).also { pause = it }
        scope.launch { container.pauseRequests.collect(pause::show) }

        // Today's live count, or null when the user has switched the counter off.
        val bubbleState = combine(container.reels.history, container.settings.settings) { history, settings ->
            if (!settings.counterBubbleEnabled) {
                null
            } else {
                BubbleState(reels = history.reels[container.reels.today()]?.values?.sum() ?: 0, budget = settings.dailyBudget)
            }
        }.stateIn(scope, SharingStarted.Eagerly, null)
        val bubble = CounterBubbleOverlay(this, bubbleState).also { bubble = it }
        scope.launch {
            bubbleState.collect { state ->
                when {
                    state == null -> bubble.hide()
                    trackedAppInFront && !keyguard.isKeyguardLocked -> bubble.show()
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        pause?.dismiss()
        bubble?.hide()
        scope.cancel()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> onWindowChanged(event.packageName?.toString())
            // Windows opening or closing (Recents, split screen, a dialog going away): recheck.
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> onWindowChanged(null)
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val packageName = event.packageName?.toString() ?: return
                // Other apps' scrolls are dropped here, before anything about them is read.
                if (TrackedApp.fromPackage(packageName) == null) return
                onTrackedScroll(event, packageName)
            }
        }
    }

    override fun onInterrupt() = Unit

    private fun onWindowChanged(packageName: String?) {
        if (packageName == this.packageName) return // Our own overlays.
        if (packageName != null && TrackedApp.fromPackage(packageName) != null) {
            setTrackedAppInFront(true)
            return
        }
        // Something else changed: a notification, the keyboard, a system dialog or another app. Only
        // the app that owns the front app window decides; system windows never hide the counter.
        val front = frontAppPackage() ?: return
        setTrackedAppInFront(TrackedApp.fromPackage(front) != null)
    }

    /** The package of the topmost app window (the launcher counts as an app), by name only. */
    private fun frontAppPackage(): String? {
        val apps = windows.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
        val front = apps.firstOrNull { it.isActive } ?: apps.firstOrNull { it.isFocused } ?: apps.firstOrNull()
        return front?.root?.packageName?.toString()
    }

    private fun setTrackedAppInFront(inFront: Boolean) {
        trackedAppInFront = inFront
        // Never on the lock screen: today's count is nobody else's business.
        if (inFront && !keyguard.isKeyguardLocked) bubble?.show() else bubble?.hide()
    }

    private val keyguard by lazy { getSystemService(KeyguardManager::class.java) }

    /** Hides the pill as the screen turns off, and brings it back once the phone is unlocked. */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> bubble?.hide()
                Intent.ACTION_USER_PRESENT -> if (trackedAppInFront) bubble?.show()
            }
        }
    }

    private fun onTrackedScroll(event: AccessibilityEvent, packageName: String) {
        val container = container ?: return
        // A scroll proves the app is in front, e.g. when the service starts while it's open.
        if (!trackedAppInFront || bubble?.isShowing == false) setTrackedAppInFront(true)
        val signal = event.toScrollSignal(packageName)
        val decision = detector.decide(signal)
        container.diagnostics.record(signal, decision)
        if (debuggable) {
            // Debug builds only, and structure only: lets `adb logcat -s ReclaimCounter` show what the counter decides.
            Log.d(
                "ReclaimCounter",
                "${decision.name} ${signal.packageName} pos=${signal.fromIndex}..${signal.toIndex}/${signal.itemCount} " +
                    "h=${signal.containerHeight}/${signal.screenHeight} grid=${signal.rowCount}x${signal.columnCount} " +
                    "scroll=${signal.scrollY}/${signal.maxScrollY} dy=${signal.scrollDeltaY} " +
                    "id=${signal.viewId} class=${signal.className}",
            )
        }
        TrackedApp.fromPackage(packageName)?.let { app -> repeat(decision.reels) { container.recordReel(app) } }
    }

    private fun AccessibilityEvent.toScrollSignal(packageName: String): ScrollSignal {
        val node = source
        val bounds = Rect().also { node?.getBoundsInScreen(it) }
        val collection = node?.collectionInfo
        return ScrollSignal(
            packageName = packageName,
            className = className?.toString(),
            viewId = node?.viewIdResourceName,
            fromIndex = fromIndex,
            toIndex = toIndex,
            itemCount = itemCount,
            containerHeight = if (node == null) -1 else bounds.height(),
            screenHeight = resources.displayMetrics.heightPixels,
            rowCount = collection?.rowCount ?: -1,
            columnCount = collection?.columnCount ?: -1,
            timeMillis = eventTime,
            scrollY = scrollY,
            maxScrollY = maxScrollY,
            scrollDeltaY = scrollDeltaY,
        )
    }
}
