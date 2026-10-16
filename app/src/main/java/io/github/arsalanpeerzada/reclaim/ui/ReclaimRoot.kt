package io.github.arsalanpeerzada.reclaim.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.arsalanpeerzada.reclaim.AppContainer
import io.github.arsalanpeerzada.reclaim.R
import io.github.arsalanpeerzada.reclaim.counting.isCountingEnabled
import io.github.arsalanpeerzada.reclaim.ui.checkin.CheckInScreen
import io.github.arsalanpeerzada.reclaim.ui.components.FloatingNav
import io.github.arsalanpeerzada.reclaim.ui.components.NavItem
import io.github.arsalanpeerzada.reclaim.ui.onboarding.OnboardingScreen
import io.github.arsalanpeerzada.reclaim.ui.quests.QuestsScreen
import io.github.arsalanpeerzada.reclaim.ui.reflection.ReflectionScreen
import io.github.arsalanpeerzada.reclaim.ui.settings.DiagnosticsScreen
import io.github.arsalanpeerzada.reclaim.ui.settings.SettingsScreen
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import io.github.arsalanpeerzada.reclaim.ui.today.TodayScreen
import io.github.arsalanpeerzada.reclaim.ui.world.WorldScreen
import kotlinx.coroutines.launch

private enum class Screen { Today, World, Quests, Settings, Diagnostics, Reflection, CheckIn }

@Composable
fun ReclaimRoot(container: AppContainer) {
    val settings by container.settings.settings.collectAsState(initial = null)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Re-checked on every resume, as the user switches the service on in system settings.
    var countingEnabled by remember { mutableStateOf(isCountingEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        countingEnabled = isCountingEnabled(context)
        onPauseOrDispose { }
    }
    val openAccessibilitySettings = {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    Box(Modifier.fillMaxSize().background(ReclaimTheme.palette.ground)) {
        val current = settings ?: return@Box
        if (!current.onboarded) {
            OnboardingScreen(
                countingEnabled = countingEnabled,
                statsAvailable = container.stats.available,
                onOpenAccessibilitySettings = openAccessibilitySettings,
                onShareChoice = { stats, wellbeing ->
                    if (stats) scope.launch { container.stats.enable(wellbeing) }
                },
                onFinish = { budget, typical ->
                    scope.launch { container.settings.completeOnboarding(budget, typical, container.reels.today()) }
                },
            )
            return@Box
        }

        var screen by rememberSaveable { mutableStateOf(Screen.Today) }
        BackHandler(enabled = screen != Screen.Today) {
            screen = if (screen == Screen.Diagnostics) Screen.Settings else Screen.Today
        }
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn(tween(240)) + slideInVertically(tween(260)) { it / 24 })
                    .togetherWith(fadeOut(tween(160)))
            },
            label = "screen",
        ) { shown ->
        when (shown) {
            Screen.Today -> TodayScreen(
                container = container,
                countingEnabled = countingEnabled,
                // Via Settings, so the user sees the disclosure and agrees before the system screen.
                onOpenAccessibilitySettings = { screen = Screen.Settings },
                onOpenReflection = { screen = Screen.Reflection },
                onOpenCheckIn = { screen = Screen.CheckIn },
            )
            Screen.Reflection -> ReflectionScreen(container, onBack = { screen = Screen.Today })
            Screen.CheckIn -> CheckInScreen(container, onBack = { screen = Screen.Today })
            Screen.World -> WorldScreen(container)
            Screen.Quests -> QuestsScreen(container)
            Screen.Settings -> SettingsScreen(
                settings = current,
                countingEnabled = countingEnabled,
                onSave = { budget, typical -> scope.launch { container.reels.updateGoals(budget, typical) } },
                onPauseEnabledChange = { enabled -> scope.launch { container.settings.setPauseEnabled(enabled) } },
                onCounterBubbleChange = { enabled -> scope.launch { container.settings.setCounterBubbleEnabled(enabled) } },
                onCommitmentLockChange = { enabled -> scope.launch { container.settings.setCommitmentLock(enabled) } },
                statsAvailable = container.stats.available,
                onSharingChange = { _, wellbeing -> scope.launch { container.stats.enable(wellbeing) } },
                onStopSharing = { scope.launch { runCatching { container.stats.stopAndDelete() } } },
                onDeleteAllData = {
                    scope.launch {
                        container.deleteAllData()
                        screen = Screen.Today
                    }
                },
                onOpenAccessibilitySettings = openAccessibilitySettings,
                onOpenDiagnostics = { screen = Screen.Diagnostics },
            )
            Screen.Diagnostics -> DiagnosticsScreen(
                diagnostics = container.diagnostics,
                onBack = { screen = Screen.Settings },
            )
        }
        }
        // Sub-screens have their own back button instead of the bar.
        if (screen in setOf(Screen.Today, Screen.World, Screen.Quests, Screen.Settings)) {
            FloatingNav(
                items = listOf(
                    NavItem("Today", R.drawable.ic_home, screen == Screen.Today) { screen = Screen.Today },
                    NavItem("Garden", R.drawable.ic_world, screen == Screen.World) { screen = Screen.World },
                    NavItem("Quests", R.drawable.ic_quest, screen == Screen.Quests) { screen = Screen.Quests },
                    NavItem("Settings", R.drawable.ic_settings, screen == Screen.Settings) { screen = Screen.Settings },
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
