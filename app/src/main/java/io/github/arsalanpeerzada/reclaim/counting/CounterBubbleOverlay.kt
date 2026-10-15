package io.github.arsalanpeerzada.reclaim.counting

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import io.github.arsalanpeerzada.reclaim.ui.bubble.BubbleState
import io.github.arsalanpeerzada.reclaim.ui.bubble.CounterBubble
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * The live counter: a small pill at the top centre of the screen while Instagram or TikTok is open.
 * Its window is only as big as the pill and lets every touch outside it through to the app below.
 */
class CounterBubbleOverlay(private val service: AccessibilityService, private val state: StateFlow<BubbleState?>) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val window = OverlayWindow(windowManager)

    fun show() {
        if (window.isShowing || state.value == null) return
        val view = ComposeView(service).apply {
            setContent {
                val current by state.collectAsState()
                ReclaimTheme { current?.let { CounterBubble(it) } }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Just below the status bar, so the clock and notification icons stay visible.
            y = statusBarHeight() + (6 * service.resources.displayMetrics.density).toInt()
        }
        window.show(view, params)
    }

    val isShowing: Boolean get() = window.isShowing

    fun hide() = window.hide()

    private fun statusBarHeight(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.windowInsets.getInsets(WindowInsets.Type.statusBars()).top
    } else {
        (24 * service.resources.displayMetrics.density).toInt()
    }
}
