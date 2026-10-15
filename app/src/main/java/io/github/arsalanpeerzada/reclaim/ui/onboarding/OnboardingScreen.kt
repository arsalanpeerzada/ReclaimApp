package io.github.arsalanpeerzada.reclaim.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import io.github.arsalanpeerzada.reclaim.ui.components.ShareDetails
import io.github.arsalanpeerzada.reclaim.ui.components.StatTile
import io.github.arsalanpeerzada.reclaim.ui.components.StepSegments
import io.github.arsalanpeerzada.reclaim.ui.components.StepperCard
import io.github.arsalanpeerzada.reclaim.ui.components.TextLink
import io.github.arsalanpeerzada.reclaim.ui.components.budgetRange
import io.github.arsalanpeerzada.reclaim.ui.components.reelMinutes
import io.github.arsalanpeerzada.reclaim.ui.components.rememberReducedMotion
import io.github.arsalanpeerzada.reclaim.ui.components.suggestedBudget
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme

private enum class Step { Welcome, Usual, Budget, Share, Counting }

@Composable
fun OnboardingScreen(
    countingEnabled: Boolean,
    statsAvailable: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onShareChoice: (stats: Boolean, wellbeing: Boolean) -> Unit,
    onFinish: (budget: Int, typical: Int) -> Unit,
) {
    // The sharing question only appears when this build has a Firebase project to share with.
    val steps = remember(statsAvailable) { Step.entries.filter { it != Step.Share || statsAvailable } }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var typical by rememberSaveable { mutableIntStateOf(Settings.DEFAULT_TYPICAL) }
    var budget by rememberSaveable { mutableIntStateOf(suggestedBudget(Settings.DEFAULT_TYPICAL)) }
    val step = steps[index.coerceIn(steps.indices)]
    BackHandler(enabled = index > 0) { index-- }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            AnimatedVisibility(visible = index > 0, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                CircleIconButton(R.drawable.ic_back, "Back", { index-- })
            }
            StepSegments(count = steps.size, reached = index + 1, modifier = Modifier.weight(1f))
        }
        // Steps slide in from the side they come from: forwards from the right, back from the left.
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(320)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(320)))
                    .togetherWith(slideOutHorizontally(tween(220)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(220)))
            },
            label = "onboarding step",
            modifier = Modifier.weight(1f),
        ) { shown ->
            val number = shown + 1
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when (steps[shown.coerceIn(steps.indices)]) {
                    Step.Welcome -> WelcomeStep()
                    Step.Usual -> {
                        StepIntro(number, steps.size, "What's a normal day?", "Roughly how many reels do you watch on a typical day? A guess is fine: it's the baseline for the time you win back.")
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
                    Step.Budget -> {
                        StepIntro(number, steps.size, "Choose your daily budget", "Pick a limit that feels doable. You can change it any time, and a freeze covers the odd day over.")
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
                    Step.Share -> ShareStep(
                        number = number,
                        total = steps.size,
                        onChoice = { stats, wellbeing ->
                            onShareChoice(stats, wellbeing)
                            index++
                        },
                    )
                    Step.Counting -> if (countingEnabled) {
                        CountingOnStep()
                    } else {
                        StepIntro(null, steps.size, "Switch on counting", "Reclaim uses an Android accessibility service. Here is exactly what it can and can't see.")
                        PermissionCards()
                    }
                }
            }
        }

        when {
            // The sharing step has its own yes and no.
            step == Step.Share -> Unit
            step != Step.Counting -> PillButton(if (index == 0) "Let's go" else "Continue", { index++ }, badge = Bright.cycle[(index + 1) % Bright.cycle.size])
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
private fun StepIntro(number: Int?, total: Int, title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (number != null) Chip("STEP $number OF $total", ReclaimTheme.palette.softMint)
        ScreenTitle(title)
        BodyText(body)
    }
}

/**
 * Opt-in for anonymous stats: off unless the user says yes, with wellbeing data as its own explicit
 * choice. Says exactly what's shared and what never is.
 */
@Composable
private fun ColumnScope.ShareStep(number: Int, total: Int, onChoice: (stats: Boolean, wellbeing: Boolean) -> Unit) {
    var wellbeing by rememberSaveable { mutableStateOf(false) }
    StepIntro(
        number, total, "Help prove Reclaim works",
        "Share anonymous daily totals so we can show that Reclaim really helps people scroll less. It's optional, and you can stop and delete it any time in Settings.",
    )
    ShareDetails()
    Row(
        Modifier.fillMaxWidth().clickable { wellbeing = !wellbeing },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Also share my mood tags and wellbeing check-in scores",
            style = MaterialTheme.typography.bodyLarge,
            color = ReclaimTheme.palette.text,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = wellbeing, onCheckedChange = { wellbeing = it })
    }
    PillButton("Yes, share anonymously", { onChoice(true, wellbeing) }, badge = Bright.Mint)
    TextLink("No thanks", { onChoice(false, false) }, Modifier.align(Alignment.CenterHorizontally))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WelcomeStep() {
    val palette = ReclaimTheme.palette
    val still = rememberReducedMotion()
    val drift by rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "drift",
    )
    Box(Modifier.fillMaxWidth().height(340.dp), contentAlignment = Alignment.Center) {
        // Confetti: decoration only. Each dot floats on its own little orbit.
        Canvas(Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2, size.height / 2)
            val r = 7.dp.toPx()
            fun float(x: Float, y: Float, phase: Float) =
                if (still) Offset(x, y) else Offset(x + kotlin.math.cos(drift + phase) * r, y + kotlin.math.sin(drift * 2 + phase) * r)
            drawCircle(palette.softViolet, radius = 150.dp.toPx(), center = centre)
            drawCircle(palette.card, radius = 108.dp.toPx(), center = centre)
            drawCircle(Bright.Sky, radius = 14.dp.toPx(), center = float(size.width * 0.12f, size.height * 0.14f, 0f))
            drawCircle(Bright.Pink, radius = 12.dp.toPx(), center = float(size.width * 0.86f, size.height * 0.12f, 1.3f))
            drawCircle(Bright.Coral, radius = 8.dp.toPx(), center = float(size.width * 0.9f, size.height * 0.5f, 2.1f))
            drawCircle(Bright.Violet, radius = 6.dp.toPx(), center = float(size.width * 0.15f, size.height * 0.5f, 3.4f))
            drawCircle(Bright.Sunflower, radius = 13.dp.toPx(), center = float(size.width * 0.84f, size.height * 0.86f, 4.2f))
            drawCircle(Bright.Mint, radius = 10.dp.toPx(), center = float(size.width * 0.14f, size.height * 0.84f, 5.0f), style = Stroke(5.dp.toPx()))
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
