package io.github.arsalanpeerzada.reclaim.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.arsalanpeerzada.reclaim.R
import io.github.arsalanpeerzada.reclaim.data.Settings
import io.github.arsalanpeerzada.reclaim.ui.components.BodyText
import io.github.arsalanpeerzada.reclaim.ui.components.Chip
import io.github.arsalanpeerzada.reclaim.ui.components.CircleIconButton
import io.github.arsalanpeerzada.reclaim.ui.components.PermissionCards
import io.github.arsalanpeerzada.reclaim.ui.components.PillButton
import io.github.arsalanpeerzada.reclaim.ui.components.Pip
import io.github.arsalanpeerzada.reclaim.ui.components.PipMood
import io.github.arsalanpeerzada.reclaim.ui.components.ScreenTitle
import io.github.arsalanpeerzada.reclaim.ui.components.StatTile
import io.github.arsalanpeerzada.reclaim.ui.components.StepSegments
import io.github.arsalanpeerzada.reclaim.ui.components.StepperCard
import io.github.arsalanpeerzada.reclaim.ui.components.TextLink
import io.github.arsalanpeerzada.reclaim.ui.components.budgetRange
import io.github.arsalanpeerzada.reclaim.ui.components.reelMinutes
import io.github.arsalanpeerzada.reclaim.ui.components.suggestedBudget
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme

private const val STEPS = 4

@Composable
fun OnboardingScreen(
    countingEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onFinish: (budget: Int, typical: Int) -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var typical by rememberSaveable { mutableIntStateOf(Settings.DEFAULT_TYPICAL) }
    var budget by rememberSaveable { mutableIntStateOf(suggestedBudget(Settings.DEFAULT_TYPICAL)) }
    BackHandler(enabled = step > 0) { step-- }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (step > 0) CircleIconButton(R.drawable.ic_back, "Back", { step-- })
            StepSegments(count = STEPS, reached = step + 1, modifier = Modifier.weight(1f))
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (step) {
                0 -> WelcomeStep()
                1 -> {
                    StepIntro(2, "What's a normal day?", "Roughly how many reels do you watch on a typical day? A guess is fine: it's the baseline for the time you win back.")
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(R.drawable.ic_clock, "${reelMinutes(typical)}", "of reels a day, at about 5 s each", Bright.Sunflower, Modifier.weight(1f), unit = " min")
                        StatTile(R.drawable.ic_star, "%.1f".format(reelMinutes(typical) * 7 / 60f), "every week", Bright.Pink, Modifier.weight(1f), unit = " h")
                    }
                }
                2 -> {
                    StepIntro(3, "Choose your daily budget", "Pick a limit that feels doable. You can change it any time, and a freeze covers the odd day over.")
                    StepperCard(
                        value = budget,
                        onValueChange = { budget = it },
                        range = budgetRange(typical),
                        step = 5,
                        unit = "reels a day",
                        label = "Daily budget in reels",
                        endHint = "your usual $typical",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(R.drawable.ic_clock, "${reelMinutes(budget)}", "of reels a day", Bright.Sunflower, Modifier.weight(1f), unit = " min")
                        StatTile(R.drawable.ic_star, "${reelMinutes(typical - budget)}", "back each day vs. your usual", Bright.Sky, Modifier.weight(1f), unit = " min")
                    }
                }
                else -> if (countingEnabled) CountingOnStep() else {
                    StepIntro(null, "Switch on counting", "Reclaim uses an Android accessibility service. Here is exactly what it can and can't see.")
                    PermissionCards()
                }
            }
        }

        val last = step == STEPS - 1
        when {
            !last -> PillButton(if (step == 0) "Let's go" else "Continue", { step++ }, badge = Bright.cycle[step + 1])
            countingEnabled -> PillButton("Start", { onFinish(budget, typical) }, badge = Bright.Mint)
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Play policy: the disclosure above needs an affirmative consent action before the system settings.
                PillButton("Agree and open settings", onOpenAccessibilitySettings)
                TextLink("I'll do this later", { onFinish(budget, typical) })
            }
        }
    }
}

@Composable
private fun StepIntro(number: Int?, title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (number != null) Chip("STEP $number OF $STEPS", ReclaimTheme.palette.softMint)
        ScreenTitle(title)
        BodyText(body)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeStep() {
    val palette = ReclaimTheme.palette
    Box(Modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
        // Confetti: decoration only.
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2, size.height / 2)
            drawCircle(palette.softViolet, radius = 150.dp.toPx(), center = centre)
            drawCircle(palette.card, radius = 108.dp.toPx(), center = centre)
            drawCircle(Bright.Sky, radius = 14.dp.toPx(), center = Offset(size.width * 0.12f, size.height * 0.14f))
            drawCircle(Bright.Pink, radius = 12.dp.toPx(), center = Offset(size.width * 0.86f, size.height * 0.12f))
            drawCircle(Bright.Coral, radius = 8.dp.toPx(), center = Offset(size.width * 0.9f, size.height * 0.5f))
            drawCircle(Bright.Violet, radius = 6.dp.toPx(), center = Offset(size.width * 0.15f, size.height * 0.5f))
            drawCircle(Bright.Sunflower, radius = 13.dp.toPx(), center = Offset(size.width * 0.84f, size.height * 0.86f))
            drawCircle(Bright.Mint, radius = 10.dp.toPx(), center = Offset(size.width * 0.14f, size.height * 0.84f), style = Stroke(5.dp.toPx()))
        }
        Pip(PipMood.Happy, Modifier.width(176.dp).height(190.dp), description = "Pip, the Reclaim mascot, smiling")
    }
    Text(
        buildAnnotatedString {
            append("Fewer reels.\n")
            withStyle(SpanStyle(color = Bright.Violet)) { append("More you.") }
        },
        style = MaterialTheme.typography.displaySmall,
        color = palette.text,
    )
    BodyText("Reclaim counts the reels you watch in Instagram and TikTok and helps you stay under a budget you choose.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("No guilt trips", palette.softCoral)
        Chip("No hard blocks", palette.softSky)
        Chip("Streaks and rewards", palette.softMint)
    }
}

@Composable
private fun CountingOnStep() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(24.dp))
        Pip(PipMood.Proud, Modifier.size(width = 150.dp, height = 162.dp), description = "Pip, cheering")
        ScreenTitle("Counting is on")
        BodyText("Open Instagram or TikTok and your reels will start to count. Pip will keep score.")
    }
}
