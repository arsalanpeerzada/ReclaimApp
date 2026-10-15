package io.github.arsalanpeerzada.reclaim.counting

import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * A Compose view in its own accessibility overlay window. Compose needs a lifecycle and saved state,
 * which an overlay doesn't have, so each window gets its own.
 */
internal class OverlayWindow(private val windowManager: WindowManager) {
    private var view: ComposeView? = null
    private var owner: Owner? = null

    val isShowing: Boolean get() = view != null

    fun show(view: ComposeView, params: WindowManager.LayoutParams) {
        if (isShowing) return
        val owner = Owner().also { it.start() }
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
        windowManager.addView(view, params)
        this.view = view
        this.owner = owner
    }

    fun hide() {
        view?.let(windowManager::removeView)
        owner?.stop()
        view = null
        owner = null
    }

    private class Owner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

        fun start() {
            savedState.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }
}
