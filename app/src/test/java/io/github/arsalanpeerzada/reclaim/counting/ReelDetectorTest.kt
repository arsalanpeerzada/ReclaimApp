package io.github.arsalanpeerzada.reclaim.counting

import org.junit.Assert.assertEquals
import org.junit.Test

class ReelDetectorTest {
    private val detector = ReelDetector()

    private fun tiktok(from: Int, to: Int = from, height: Int = 2400, rows: Int = 50, cols: Int = 1, at: Long = 0) = ScrollSignal(
        packageName = "com.zhiliaoapp.musically",
        className = "androidx.viewpager.widget.ViewPager",
        viewId = "com.zhiliaoapp.musically:id/abc",
        fromIndex = from,
        toIndex = to,
        itemCount = 50,
        containerHeight = height,
        screenHeight = 2400,
        rowCount = rows,
        columnCount = cols,
        timeMillis = at,
    )

    private fun instagram(viewId: String?, from: Int) = tiktok(from).copy(packageName = "com.instagram.android", viewId = viewId)

    @Test
    fun `counts the first reel and each new one once`() {
        assertEquals(Decision.COUNTED, detector.decide(tiktok(0)))
        assertEquals(Decision.SAME_REEL, detector.decide(tiktok(0)))
        // Mid-swipe: reel 0 is still partly on screen.
        assertEquals(Decision.SAME_REEL, detector.decide(tiktok(0, 1)))
        assertEquals(Decision.COUNTED, detector.decide(tiktok(1)))
        assertEquals(Decision.SAME_REEL, detector.decide(tiktok(1)))
    }

    @Test
    fun `keeps counting well past any budget`() {
        repeat(300) { i -> assertEquals(Decision.COUNTED, detector.decide(tiktok(i))) }
    }

    @Test
    fun `counts pagers that always show the next reel peeking`() {
        detector.decide(tiktok(0, 1))
        assertEquals(Decision.COUNTED, detector.decide(tiktok(1, 2)))
        assertEquals(Decision.COUNTED, detector.decide(tiktok(2, 3)))
    }

    @Test
    fun `a swipe that snaps back counts nothing`() {
        detector.decide(tiktok(3))
        assertEquals(Decision.SAME_REEL, detector.decide(tiktok(3, 4)))
        assertEquals(Decision.SAME_REEL, detector.decide(tiktok(3)))
    }

    @Test
    fun `without positions, each swipe counts once`() {
        assertEquals(Decision.COUNTED, detector.decide(tiktok(-1, -1, at = 0)))
        assertEquals(Decision.SAME_SWIPE, detector.decide(tiktok(-1, -1, at = 100)))
        assertEquals(Decision.SAME_SWIPE, detector.decide(tiktok(-1, -1, at = 300)))
        assertEquals(Decision.COUNTED, detector.decide(tiktok(-1, -1, at = 2_000)))
    }

    /**
     * Event times (ms) from the Pixel 8 capture on 14 October 2026: 10 TikTok swipes, no positions,
     * offsets or deltas reported, and a follow-up scroll about a second after many of them.
     */
    @Test
    fun `TikTok's follow-up scrolls aren't counted as extra reels`() {
        val times = listOf(
            0L, 124, 1494, 1752, 2931, 3070, 8346, 8485, 11998, 12126, 17562, 21402, 21531, 22583, 22705,
            24967, 25098, 25899, 26049, 31251, 31367, 32433, 32566, 35187, 35368, 36254, 36380, 38271, 38407,
            38509, 39410, 39547, 39657, 41601, 41722,
        )
        val counted = times.sumOf { detector.decide(tiktok(-1, -1, at = it)).reels }
        // The old rule counted 17 here.
        assertEquals(11, counted)
    }

    @Test
    fun `opening Instagram on reel 1 also counts the reel before it`() {
        val reels = (1..15).sumOf { detector.decide(instagram("com.instagram.android:id/clips_viewer_view_pager", it)).reels }
        assertEquals(16, reels)
    }

    @Test
    fun `pagers that report their scroll offset count by whole screens`() {
        fun at(scrollY: Int) = tiktok(-1, -1).copy(scrollY = scrollY, maxScrollY = 2400 * 50)
        assertEquals(Decision.COUNTED, detector.decide(at(0)))
        assertEquals(Decision.SAME_SWIPE, detector.decide(at(1200)))
        assertEquals(Decision.COUNTED, detector.decide(at(2400)))
        assertEquals(Decision.SAME_REEL, detector.decide(at(2410)))
    }

    @Test
    fun `with scroll distances, small follow-up scrolls don't count`() {
        fun moved(dy: Int, at: Long) = tiktok(-1, -1, at = at).copy(scrollDeltaY = dy)
        detector.decide(moved(0, 0))
        assertEquals(Decision.SMALL_SCROLL, detector.decide(moved(500, 1_000)))
        assertEquals(Decision.COUNTED, detector.decide(moved(900, 1_100)))
        assertEquals(Decision.SAME_SWIPE, detector.decide(moved(800, 1_200)))
        // A nudge a second later: not a swipe.
        assertEquals(Decision.SMALL_SCROLL, detector.decide(moved(40, 2_300)))
    }

    @Test
    fun `ignores untracked apps, small containers, sideways pagers and unknown containers`() {
        assertEquals(Decision.NOT_TRACKED, detector.decide(tiktok(0).copy(packageName = "com.example.other")))
        assertEquals(Decision.TOO_SMALL, detector.decide(tiktok(0, height = 900)))
        assertEquals(Decision.HORIZONTAL, detector.decide(tiktok(0, rows = 1, cols = 20)))
        assertEquals(Decision.NO_SOURCE, detector.decide(tiktok(0, height = -1)))
    }

    @Test
    fun `uses Instagram's reel viewer id when the app reports one`() {
        assertEquals(Decision.OTHER_CONTAINER, detector.decide(instagram("com.instagram.android:id/list", 0)))
        assertEquals(Decision.COUNTED, detector.decide(instagram("com.instagram.android:id/clips_viewer_view_pager", 0)))
        // No id reported: fall back to the container's shape.
        assertEquals(Decision.COUNTED, detector.decide(instagram(null, 0)))
    }
}
