package io.github.arsalanpeerzada.reclaim.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetMeterTest {
    @Test
    fun `every reel moves the meter, including past the budget`() {
        // Budget 100: reel 35 fills three segments and half of the fourth.
        assertEquals(1f, BudgetMeter.firstLap(0.35f, 2), 0.001f)
        assertEquals(0.5f, BudgetMeter.firstLap(0.35f, 3), 0.001f)
        assertEquals(0f, BudgetMeter.secondLap(0.35f, 0), 0.001f)
        // 125%: first lap full, overflow lap two and a half segments in.
        assertEquals(1f, BudgetMeter.firstLap(1.25f, 9), 0.001f)
        assertEquals(1f, BudgetMeter.secondLap(1.25f, 1), 0.001f)
        assertEquals(0.5f, BudgetMeter.secondLap(1.25f, 2), 0.001f)
    }

    @Test
    fun `Pip gets steadily sadder the further over budget`() {
        val looks = listOf(0.2f, 0.9f, 1.1f, 1.5f, 2f, 2.5f, 4f).map(PipLook::forBudget)
        looks.zipWithNext().forEach { (a, b) ->
            assertTrue("joy should not rise", b.joy <= a.joy)
            assertTrue("gloom should not fall", b.gloom >= a.gloom)
        }
        assertEquals(0f, PipLook.forBudget(0.5f).gloom, 0f)
        assertEquals(1f, PipLook.forBudget(2.5f).gloom, 0.001f)
    }

    @Test
    fun `Pip's lines escalate but never blame`() {
        assertEquals("On track.", pipLine(60, 100).headline)
        assertEquals("Pip's a bit worried.", pipLine(110, 100).headline)
        assertEquals("Pip's feeling it.", pipLine(140, 100).headline)
        assertEquals("Pip's really down.", pipLine(180, 100).headline)
        assertEquals("Pip's heartbroken.", pipLine(260, 100).headline)
        val words = listOf("wasted", "shame", "should", "bad")
        listOf(60, 90, 100, 110, 140, 180, 260).forEach { reels ->
            val line = pipLine(reels, 100)
            words.forEach { assertTrue("$it in $line", it !in (line.headline + line.detail).lowercase()) }
        }
    }
}
