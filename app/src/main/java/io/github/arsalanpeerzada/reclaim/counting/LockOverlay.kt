package io.github.arsalanpeerzada.reclaim.counting

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import io.github.arsalanpeerzada.reclaim.domain.LockState
import io.github.arsalanpeerzada.reclaim.ui.pause.LockScreen
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import kotlinx.coroutines.flow.StateFlow

/** The opt-in commitment lock over Instagram and TikTok. Full screen and touch-blocking while shown. */
class LockOverlay(
    private val service: AccessibilityService,
    private val state: StateFlow<LockState?>,
    private val onUnlock: () -> Unit,
) {
    private val window = OverlayWindow(service.getSystemService(WindowManager::class.java))

    val isShowing: Boolean get() = window.isShowing

    fun show() {
        if (window.isShowing || state.value == null) return
        val view = ComposeView(service).apply {
            setContent {
                val current by state.collectAsState()
                ReclaimTheme {
                    current?.let {
                        LockScreen(
                            state = it,
                            onLeave = {
                                hide()
                                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                            },
                            onUnlock = {
                                hide()
                                onUnlock()
                            },
                        )
                    }
                }
            }
        }
        window.show(
            view,
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ),
        )
    }

    fun hide() = window.hide()
}
