package io.github.arsalanpeerzada.reclaim.counting

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The last few counting decisions, for checking accuracy on a real phone. Held in memory only:
 * never written to disk and gone when the app process ends.
 */
class CountingDiagnostics(private val capacity: Int = 80) {
    data class Entry(val atMillis: Long, val signal: ScrollSignal, val decision: Decision)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun record(signal: ScrollSignal, decision: Decision) {
        val entry = Entry(System.currentTimeMillis(), signal, decision)
        _entries.update { (listOf(entry) + it).take(capacity) }
    }

    fun clear() = _entries.update { emptyList() }
}

/** Whether the user has switched the counter on in Android's accessibility settings. */
fun isCountingEnabled(context: Context): Boolean {
    val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
        val info = it.resolveInfo.serviceInfo
        info.packageName == context.packageName && info.name == ReelCountingService::class.java.name
    }
}
