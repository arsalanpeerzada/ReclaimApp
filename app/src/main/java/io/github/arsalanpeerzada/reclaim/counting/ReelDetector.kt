package io.github.arsalanpeerzada.reclaim.counting

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * What the service passes on from one scroll event: positions and the shape of the scrolling
 * container. Deliberately nothing about what is on screen.
 */
data class ScrollSignal(
    val packageName: String,
    val className: String?,
    val viewId: String?,
    /** First and last visible item positions; -1 when the app does not report them. */
    val fromIndex: Int,
    val toIndex: Int,
    val itemCount: Int,
    /** Height of the scrolling container in pixels; -1 when the container is unknown. */
    val containerHeight: Int,
    val screenHeight: Int,
    /** From the container's collection info; -1 when it has none. */
    val rowCount: Int,
    val columnCount: Int,
    /** When the event happened, in milliseconds on any steady clock. */
    val timeMillis: Long = 0,
    /** How far the container has scrolled, and how far it can; 0 or -1 when not reported. */
    val scrollY: Int = -1,
    val maxScrollY: Int = -1,
    /** Pixels scrolled since the previous event; -1 when not reported. */
    val scrollDeltaY: Int = -1,
)

enum class Decision(val description: String, val reels: Int) {
    COUNTED("Counted", 1),

    /** The first scroll seen already sits on a later reel, so the one before it was watched too. */
    COUNTED_WITH_PREVIOUS("Counted, plus the reel before", 2),
    NOT_TRACKED("Not a tracked app", 0),
    NO_SOURCE("Container unknown", 0),
    OTHER_CONTAINER("Not the reel viewer", 0),
    TOO_SMALL("Not full screen", 0),
    HORIZONTAL("Sideways (e.g. stories)", 0),
    SAME_REEL("Same reel", 0),
    SAME_SWIPE("Same swipe", 0),
    SMALL_SCROLL("Too small to be a swipe", 0),
}

/**
 * Decides whether a scroll event means a new reel is on screen, using the best evidence the app gives:
 *
 * 1. **Positions** (Instagram): the reel on screen is the first visible position, which only changes
 *    once the previous reel has fully left. A swipe counts once when it lands; one that snaps back
 *    counts nothing.
 * 2. **Scroll offset**: a full-screen pager scrolled by N screens is on reel N, once it settles.
 * 3. **Scroll distance**: a burst of scroll events counts once it has moved at least 40% of a screen,
 *    which ignores the small follow-up scrolls some apps make after a new video starts.
 * 4. **Timing alone** (TikTok on 14 October 2026 reported none of the above): a burst counts unless it
 *    comes within [followUpMillis] of the last counted swipe; TikTok's follow-up scrolls arrived 0.9 to
 *    1.5 s after a real one.
 *
 * Feeds, grids and stories fail the shape tests. Counting never stops at the budget. Not thread-safe:
 * events arrive on the main thread.
 */
class ReelDetector(
    private val minHeightFraction: Float = 0.6f,
    private val swipeGapMillis: Long = 450,
    private val followUpMillis: Long = 1_600,
) {
    private class Container(
        var position: Int?,
        var lastEventMillis: Long,
        var lastCountedMillis: Long,
        var burstDistance: Int = 0,
        var burstCounted: Boolean = false,
    )

    private val containers = HashMap<String, Container>()

    fun decide(signal: ScrollSignal): Decision {
        val app = TrackedApp.fromPackage(signal.packageName) ?: return Decision.NOT_TRACKED
        if (signal.containerHeight <= 0) return Decision.NO_SOURCE
        if (app.reelContainerIds.isNotEmpty() && signal.viewId != null && signal.viewId !in app.reelContainerIds) {
            return Decision.OTHER_CONTAINER
        }
        if (signal.screenHeight > 0 && signal.containerHeight < signal.screenHeight * minHeightFraction) {
            return Decision.TOO_SMALL
        }
        if (signal.columnCount > 1 && signal.rowCount <= 1) return Decision.HORIZONTAL

        val key = "${signal.packageName}|${signal.viewId ?: signal.className}"
        val position = positionOf(signal)
        val container = containers[key]
        if (container == null) {
            containers[key] = Container(position, signal.timeMillis, signal.timeMillis)
            return if (position != null && position > 0) Decision.COUNTED_WITH_PREVIOUS else Decision.COUNTED
        }

        // 1 and 2: a known position.
        if (position != null) {
            container.lastEventMillis = signal.timeMillis
            val previous = container.position
            container.position = position
            return if (position != previous) counted(container, signal) else Decision.SAME_REEL
        }
        if (signal.scrollY >= 0 && signal.maxScrollY > 0) {
            // Offsets are reported but the pager is between reels: wait for it to settle.
            container.lastEventMillis = signal.timeMillis
            return Decision.SAME_SWIPE
        }

        // 3 and 4: bursts of scroll events.
        if (signal.timeMillis - container.lastEventMillis >= swipeGapMillis) {
            container.burstDistance = 0
            container.burstCounted = false
        }
        container.lastEventMillis = signal.timeMillis
        if (container.burstCounted) return Decision.SAME_SWIPE

        if (signal.scrollDeltaY > 0 || signal.scrollDeltaY < -1) {
            container.burstDistance += abs(signal.scrollDeltaY)
            return if (container.burstDistance >= signal.containerHeight * 0.4f) {
                container.burstCounted = true
                counted(container, signal)
            } else {
                Decision.SMALL_SCROLL
            }
        }
        if (signal.timeMillis - container.lastCountedMillis < followUpMillis) {
            container.burstCounted = true
            return Decision.SAME_SWIPE
        }
        container.burstCounted = true
        return counted(container, signal)
    }

    private fun counted(container: Container, signal: ScrollSignal): Decision {
        container.lastCountedMillis = signal.timeMillis
        return Decision.COUNTED
    }

    /** The reel on screen, if the app says: its position, or whole screens scrolled once settled. */
    private fun positionOf(signal: ScrollSignal): Int? {
        if (signal.fromIndex >= 0) return signal.fromIndex
        if (signal.scrollY >= 0 && signal.maxScrollY > 0) {
            val page = signal.scrollY.toFloat() / signal.containerHeight
            val nearest = page.roundToInt()
            return if (abs(page - nearest) <= 0.08f) nearest else null
        }
        return null
    }
}
