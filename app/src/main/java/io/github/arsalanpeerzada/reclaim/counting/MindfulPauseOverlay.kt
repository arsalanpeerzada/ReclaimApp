package io.github.arsalanpeerzada.reclaim.counting

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import io.github.arsalanpeerzada.reclaim.domain.PauseRequest
import io.github.arsalanpeerzada.reclaim.ui.pause.MindfulPauseScreen
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme

/**
 * Shows the mindful pause over Instagram or TikTok. An accessibility overlay needs no extra
 * permission. It is friction, not a block: after one breath the user chooses.
 */
class MindfulPauseOverlay(private val service: AccessibilityService) {
    private val window = OverlayWindow(service.getSystemService(WindowManager::class.java))

    fun show(request: PauseRequest) {
        if (window.isShowing) return
        val view = ComposeView(service).apply {
            setContent {
                ReclaimTheme {
                    MindfulPauseScreen(
                        request = request,
                        onDone = {
                            dismiss()
                            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                        },
                        onKeepWatching = ::dismiss,
                    )
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

    fun dismiss() = window.hide()
}
