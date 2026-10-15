package io.github.arsalanpeerzada.reclaim.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.arsalanpeerzada.reclaim.data.Settings
import io.github.arsalanpeerzada.reclaim.ui.components.FloatingNavClearance
import io.github.arsalanpeerzada.reclaim.ui.components.GameCard
import io.github.arsalanpeerzada.reclaim.ui.components.PermissionCards
import io.github.arsalanpeerzada.reclaim.ui.components.PillButton
import io.github.arsalanpeerzada.reclaim.ui.components.PipMood
import io.github.arsalanpeerzada.reclaim.ui.components.PipSays
import io.github.arsalanpeerzada.reclaim.ui.components.ScreenTitle
import io.github.arsalanpeerzada.reclaim.ui.components.StepperCard
import io.github.arsalanpeerzada.reclaim.ui.components.budgetRange
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme

@Composable
fun SettingsScreen(
    settings: Settings,
    countingEnabled: Boolean,
    onSave: (budget: Int, typical: Int) -> Unit,
    onPauseEnabledChange: (Boolean) -> Unit,
    onCounterBubbleChange: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val palette = ReclaimTheme.palette
    var typical by rememberSaveable { mutableIntStateOf(settings.typicalReelsPerDay) }
    var budget by rememberSaveable { mutableIntStateOf(settings.dailyBudget) }
    val changed = typical != settings.typicalReelsPerDay || budget != settings.dailyBudget

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = FloatingNavClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Settings")

        Section("Daily budget", "A new budget applies from today. Earlier days keep the budget they had.")
        StepperCard(
            value = budget,
            onValueChange = { budget = it },
            range = budgetRange(typical),
            step = 5,
            unit = "reels a day",
            label = "Daily budget in reels",
            endHint = "your usual $typical",
        )
        Section("Your usual day", "The baseline for the time you win back.")
        StepperCard(
            value = typical,
            onValueChange = {
                typical = it
                budget = budget.coerceIn(budgetRange(it))
            },
            range = 20..600,
            step = 10,
            unit = "reels a day",
            label = "Reels on a typical day",
        )
        PillButton("Save", { onSave(budget, typical) }, badge = Bright.Mint, enabled = changed)

        ToggleCard(
            title = "Live counter",
            body = "A small pill at the top centre of Instagram and TikTok with today's count. Tap it for how many are left.",
            checked = settings.counterBubbleEnabled,
            onCheckedChange = onCounterBubbleChange,
        )
        ToggleCard(
            title = "Mindful pause",
            body = "At 80% of your budget, and again when it's used up, a short pause asks \"Still choosing this?\" " +
                "and takes one breath with you. You can always keep watching.",
            checked = settings.pauseEnabled,
            onCheckedChange = onPauseEnabledChange,
        )

        GameCard(Modifier.fillMaxWidth(), colour = if (countingEnabled) palette.softMint else palette.softCoral) {
            if (countingEnabled) {
                PipSays(PipMood.Happy, "Counting is on", "Pip is keeping score in Instagram and TikTok.")
            } else {
                PipSays(PipMood.Focused, "Counting is off", "Reels aren't counted until you switch Reclaim on.")
            }
            PermissionCards()
            PillButton(if (countingEnabled) "Open accessibility settings" else "Agree and open settings", onOpenAccessibilitySettings)
        }

        GameCard(Modifier.fillMaxWidth()) {
            Text("Counting diagnostics", style = MaterialTheme.typography.titleLarge, color = palette.text)
            Text(
                "See what the counter decides for each scroll, to check it counts every reel once and nothing else.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
            )
            PillButton("Open diagnostics", onOpenDiagnostics, badge = Bright.Sky)
        }

        var confirmDelete by rememberSaveable { mutableStateOf(false) }
        GameCard(Modifier.fillMaxWidth(), colour = palette.softCoral) {
            Text("Your data", style = MaterialTheme.typography.titleLarge, color = palette.text)
            Text(
                "Everything Reclaim knows is on this phone: daily counts, budgets, quests, moods and check-ins. " +
                    "Nothing is uploaded and there are no backups. Deleting it starts Reclaim afresh.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text,
            )
            PillButton("Delete all my data", { confirmDelete = true }, badge = Bright.Coral)
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete all your data?") },
                text = {
                    Text(
                        "This removes every count, budget, streak, garden plant, quest, mood and check-in, and all settings. " +
                            "It can't be undone.",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        onDeleteAllData()
                    }) { Text("Delete everything") }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep my data") } },
            )
        }
    }
}

@Composable
private fun ToggleCard(title: String, body: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val palette = ReclaimTheme.palette
    GameCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = palette.text)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Bright.Violet, checkedThumbColor = Color.White),
                modifier = Modifier.semantics { contentDescription = title },
            )
        }
    }
}

@Composable
private fun Section(title: String, note: String) {
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = ReclaimTheme.palette.text)
        Text(note, style = MaterialTheme.typography.bodySmall, color = ReclaimTheme.palette.textMuted)
    }
}
