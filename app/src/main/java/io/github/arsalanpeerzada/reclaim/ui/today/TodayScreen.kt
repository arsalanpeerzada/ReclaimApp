package io.github.arsalanpeerzada.reclaim.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.arsalanpeerzada.reclaim.AppContainer
import io.github.arsalanpeerzada.reclaim.R
import io.github.arsalanpeerzada.reclaim.counting.TrackedApp
import io.github.arsalanpeerzada.reclaim.domain.DayOutcome
import io.github.arsalanpeerzada.reclaim.domain.Level
import io.github.arsalanpeerzada.reclaim.domain.Moods
import io.github.arsalanpeerzada.reclaim.domain.Overview
import io.github.arsalanpeerzada.reclaim.domain.Progress
import io.github.arsalanpeerzada.reclaim.domain.Streaks
import io.github.arsalanpeerzada.reclaim.ui.components.Appear
import io.github.arsalanpeerzada.reclaim.ui.components.BudgetMeter
import io.github.arsalanpeerzada.reclaim.ui.components.ConfettiBurst
import io.github.arsalanpeerzada.reclaim.ui.components.FloatingNavClearance
import io.github.arsalanpeerzada.reclaim.ui.components.GameCard
import io.github.arsalanpeerzada.reclaim.ui.components.MoodFaces
import io.github.arsalanpeerzada.reclaim.ui.components.PillButton
import io.github.arsalanpeerzada.reclaim.ui.components.Pip
import io.github.arsalanpeerzada.reclaim.ui.components.PipLook
import io.github.arsalanpeerzada.reclaim.ui.components.PipMood
import io.github.arsalanpeerzada.reclaim.ui.components.PipSays
import io.github.arsalanpeerzada.reclaim.ui.components.RollingNumber
import io.github.arsalanpeerzada.reclaim.ui.components.StatTile
import io.github.arsalanpeerzada.reclaim.ui.components.TextLink
import io.github.arsalanpeerzada.reclaim.ui.components.pipLine
import io.github.arsalanpeerzada.reclaim.ui.components.popIn
import io.github.arsalanpeerzada.reclaim.ui.components.rememberReducedMotion
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TodayScreen(
    container: AppContainer,
    countingEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenCheckIn: () -> Unit,
) {
    val viewModel = viewModel { TodayViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDate()
        onPauseOrDispose { }
    }
    val current = state ?: return
    val overview = current.overview

    Column(
        Modifier
            .fillMaxSize()
            // Below the status bar before scrolling, so content never slides under the clock.
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = FloatingNavClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Each block rises in a beat after the one above it.
        Appear(0) { Header(overview) }
        if (!countingEnabled) Appear(1) { CountingOffCard(onOpenAccessibilitySettings) }
        Appear(1) {
            GraduationCard(current.graduation, current.typicalReelsPerDay, onGraduate = {
                viewModel.graduate()
                // Graduating means not needing the counter: offer to switch it off right away.
                onOpenAccessibilitySettings()
            }, onNotYet = viewModel::notYet)
        }
        if (current.reflectionDue) {
            Appear(2) {
                PromptCard(
                    ReclaimTheme.palette.softSky, PipMood.Happy, "Your weekly reflection is ready",
                    "Look back at last week and choose next week's budget.", "Reflect", Bright.Sky, onOpenReflection,
                )
            }
        }
        Appear(2) { RingCard(overview) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(R.drawable.ic_flame, "${overview.streak.current}", "day streak", Bright.Coral, Modifier.weight(1f).popIn(3))
            StatTile(
                R.drawable.ic_snowflake, "${overview.streak.freezesLeft}", "freezes saved", Bright.Sky, Modifier.weight(1f).popIn(4),
                unit = "/${Streaks.MAX_FREEZES}",
            )
            StatTile(R.drawable.ic_clock, "${overview.reclaimed.minutes}", "won back", Bright.Sunflower, Modifier.weight(1f).popIn(5), unit = " min")
        }
        Appear(4) { MoodCard(current.mood, viewModel::setMood) }
        Appear(5) { NextUpCard(overview) }
        Appear(6) { WeekCard(overview.lastSevenDays) }
        if (current.checkInDue) {
            Appear(7) {
                PromptCard(
                    ReclaimTheme.palette.softPink, PipMood.Happy, "Monthly check-in",
                    "Five quick questions about how you've been. It takes a minute.", "Start check-in", Bright.Pink, onOpenCheckIn,
                )
            }
        }
    }
    current.levelUp?.let { level -> LevelUpDialog(level) { viewModel.celebrated(level) } }
}

/** A level reached: Pip cheers and confetti flies. Shown once per level. */
@Composable
private fun LevelUpDialog(level: Level, onDismiss: () -> Unit) {
    val palette = ReclaimTheme.palette
    Dialog(onDismissRequest = onDismiss) {
        Box(contentAlignment = Alignment.Center) {
            GameCard(Modifier.fillMaxWidth().popIn(), colour = palette.ground) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Pip(PipMood.Proud, Modifier.size(width = 110.dp, height = 120.dp), description = "Pip, cheering")
                    Text("Level ${level.number}!", style = MaterialTheme.typography.displaySmall, color = Bright.Violet)
                    Text("You're ${level.name}", style = MaterialTheme.typography.headlineSmall, color = palette.text)
                    Text(
                        level.daysToNext?.let { "${it} more days within budget to reach the next level." } ?: "That's the top level. Pip's very proud.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                        textAlign = TextAlign.Center,
                    )
                    PillButton("Nice!", onDismiss, badge = Bright.Sunflower)
                }
            }
            ConfettiBurst(trigger = level.number, modifier = Modifier.matchParentSize(), pieces = 60)
        }
    }
}

/** The design doc's best outcome: the user no longer needs the app. Counted as a win, not churn. */
@Composable
private fun GraduationCard(graduation: Graduation, typical: Int, onGraduate: () -> Unit, onNotYet: () -> Unit) {
    val palette = ReclaimTheme.palette
    when (graduation) {
        Graduation.NotYet -> Unit
        is Graduation.Ready -> GameCard(Modifier.fillMaxWidth(), colour = palette.softMint) {
            PipSays(
                PipMood.Proud,
                "You might not need Reclaim any more",
                "${graduation.trend.percentWithinBudget}% of the last 30 days within budget, at about " +
                    "${graduation.trend.averagePerDay} reels a day against your usual $typical. That's what graduating looks like.",
            )
            PillButton("Graduate", onGraduate, badge = Bright.Mint)
            TextLink("Not yet", onNotYet, Modifier.align(Alignment.CenterHorizontally))
        }
        is Graduation.Graduated -> GameCard(Modifier.fillMaxWidth(), colour = palette.softMint, padding = 16.dp) {
            PipSays(
                PipMood.Proud,
                "Graduated on ${graduation.on.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault()))}",
                "Reclaim did its job. It's here whenever you want it again.",
            )
        }
    }
}

@Composable
private fun PromptCard(container: Color, mood: PipMood, title: String, body: String, action: String, badge: Color, onClick: () -> Unit) {
    GameCard(Modifier.fillMaxWidth(), colour = container) {
        PipSays(mood, title, body)
        PillButton(action, onClick, badge = badge)
    }
}

/** The daily mood tag: one tap, for the weekly mood insight. */
@Composable
private fun MoodCard(mood: Int?, onSelect: (Int) -> Unit) {
    GameCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Text(
            if (mood == null) "How are you feeling today?" else "Feeling ${Moods.labels[mood - 1].lowercase()} today",
            style = MaterialTheme.typography.titleLarge,
            color = ReclaimTheme.palette.text,
        )
        MoodFaces(selected = mood, onSelect = onSelect)
    }
}

@Composable
private fun Header(overview: Overview) {
    val palette = ReclaimTheme.palette
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                overview.today.date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())),
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
            )
            Text("Today", style = MaterialTheme.typography.headlineMedium, color = palette.text)
        }
        Row(
            Modifier.background(Bright.Violet, CircleShape).padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(painterResource(R.drawable.ic_star), contentDescription = null, tint = Bright.Sunflower, modifier = Modifier.size(18.dp))
            Text("Lv ${overview.level.number} · ${overview.level.name}", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
private fun CountingOffCard(onOpenAccessibilitySettings: () -> Unit) {
    GameCard(colour = ReclaimTheme.palette.softCoral) {
        PipSays(PipMood.Focused, "Counting is off", "Switch Reclaim on in accessibility settings and Pip can start keeping score.")
        PillButton("Switch on", onOpenAccessibilitySettings, badge = Bright.Coral)
    }
}

@Composable
private fun RingCard(overview: Overview) {
    val palette = ReclaimTheme.palette
    val today = overview.today
    GameCard(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SegmentedRing(
                fraction = overview.todayFraction,
                modifier = Modifier.size(196.dp).semantics { contentDescription = "${today.reels} of ${today.budget} reels used today" },
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RollingNumber(
                    "${today.reels}",
                    MaterialTheme.typography.displayMedium,
                    if (today.reels > today.budget) palette.overflow else palette.text,
                )
                Text("of ${today.budget} reels", style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
            }
        }
        // Pip's look follows the budget: the further over, the sadder he gets (and he's fine again tomorrow).
        val line = pipLine(today.reels, today.budget)
        PipSays(PipLook.forBudget(overview.todayFraction), line.headline, line.detail, pipSize = 72.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppChip(TrackedApp.INSTAGRAM, overview.todayByApp[TrackedApp.INSTAGRAM] ?: 0, Bright.Pink, palette.softPink, Modifier.weight(1f))
            AppChip(TrackedApp.TIKTOK, overview.todayByApp[TrackedApp.TIKTOK] ?: 0, Bright.Sky, palette.softSky, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AppChip(app: TrackedApp, count: Int, dot: Color, container: Color, modifier: Modifier) {
    Row(
        modifier.background(container, CircleShape).padding(vertical = 8.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(dot, CircleShape))
        Text("${app.displayName} ", style = MaterialTheme.typography.bodyMedium, color = ReclaimTheme.palette.text)
        Text("$count", style = MaterialTheme.typography.labelMedium, color = ReclaimTheme.palette.text)
    }
}

/**
 * The budget meter as a ring: ten segments, each a tenth of the budget, filling part by part with
 * every reel. Past the budget a second, inner lap fills in plum, so the ring keeps counting.
 */
@Composable
private fun SegmentedRing(fraction: Float, modifier: Modifier = Modifier) {
    val track = ReclaimTheme.palette.track
    val overflow = ReclaimTheme.palette.overflow
    val animated by animateFloatAsState(fraction, tween(600), label = "ring")
    Canvas(modifier) {
        val stroke = 18.dp.toPx()
        val gapDegrees = 4f
        val sweep = 36f - gapDegrees
        fun lap(inset: Float, width: Float, fill: (Int) -> Float, colour: (Int) -> Color, background: Color?) {
            val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
            for (i in 0 until BudgetMeter.SEGMENTS) {
                val start = -90f + i * 36f + gapDegrees / 2
                if (background != null) {
                    drawArc(background, start, sweep, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(width))
                }
                val f = fill(i)
                if (f > 0f) {
                    drawArc(colour(i), start, sweep * f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(width))
                }
            }
        }
        lap(stroke / 2, stroke, { BudgetMeter.firstLap(animated, it) }, { BudgetMeter.colours[it] }, track)
        if (animated > 1f) {
            val inner = 8.dp.toPx()
            lap(stroke + 6.dp.toPx() + inner / 2, inner, { BudgetMeter.secondLap(animated, it) }, { overflow }, null)
        }
    }
}

/** What the next small win is: a freeze, a level, or the time won back so far. */
@Composable
private fun NextUpCard(overview: Overview) {
    val palette = ReclaimTheme.palette
    val streak = overview.streak
    val level = overview.level
    GameCard(Modifier.fillMaxWidth(), colour = palette.softViolet, padding = 16.dp) {
        Text("Next up", style = MaterialTheme.typography.titleLarge, color = palette.text)
        NextUpLine(
            Bright.Sky,
            if (streak.freezesLeft >= Streaks.MAX_FREEZES) {
                "Freezes are full. A freeze covers one day over budget."
            } else {
                "A new freeze in ${days(streak.daysToNextFreeze)} under budget."
            },
        )
        NextUpLine(
            Bright.Violet,
            level.daysToNext?.let { "Level ${level.number + 1} in ${days(it)} under budget." } ?: "Top level reached: ${level.name}.",
        )
        overview.reclaimed.equivalent?.let { NextUpLine(Bright.Sunflower, "Time won back so far: about $it.") }
    }
}

private fun days(n: Int) = if (n == 1) "1 day" else "$n days"

@Composable
private fun NextUpLine(dot: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(12.dp).background(dot, CircleShape))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = ReclaimTheme.palette.text)
    }
}

@Composable
private fun WeekCard(days: List<DayOutcome>) {
    val palette = ReclaimTheme.palette
    // Always seven slots; days before counting started stay empty.
    val slots: List<DayOutcome?> = List(7 - days.size) { null } + days
    val today = days.last()
    val scale = (days.maxOf { maxOf(it.reels, it.budget) } * 1.2f).coerceAtLeast(1f)

    GameCard(Modifier.fillMaxWidth(), padding = 16.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("This week", style = MaterialTheme.typography.titleLarge, color = palette.text, modifier = Modifier.weight(1f))
            Text("${days.count { it.withinBudget }} of ${days.size} days in budget", style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
        }
        Box(Modifier.fillMaxWidth().height(96.dp)) {
            // Today's budget as a dashed line across the bars.
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1 - today.budget / scale)
                drawLine(
                    palette.textMuted.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                )
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                slots.forEachIndexed { index, day ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        if (day == null) {
                            Box(Modifier.fillMaxWidth().height(6.dp).background(palette.track, CircleShape))
                        } else {
                            Bar(day, Bright.cycle[index % Bright.cycle.size], isToday = day == today, heightFraction = (day.reels / scale).coerceIn(0.06f, 1f), order = index)
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            slots.forEachIndexed { index, day ->
                val date = day?.date ?: today.date.minusDays((slots.lastIndex - index).toLong())
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day == today) palette.text else palette.textMuted,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text("Striped bars went over budget. That's what freezes are for.", style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
    }
}

@Composable
private fun Bar(day: DayOutcome, colour: Color, isToday: Boolean, heightFraction: Float, order: Int) {
    // Bars grow up from the floor one after another, and glide when the count changes.
    val reduced = rememberReducedMotion()
    val grown = remember { Animatable(if (reduced) heightFraction else 0f) }
    LaunchedEffect(heightFraction) {
        if (grown.value == 0f) kotlinx.coroutines.delay(order * 60L)
        grown.animateTo(heightFraction, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow))
    }
    val ringed = if (isToday) Modifier.border(2.dp, ReclaimTheme.palette.text, CircleShape).padding(3.dp) else Modifier
    Box(Modifier.fillMaxWidth().fillMaxHeight(grown.value.coerceIn(0.001f, 1f)).then(ringed).clip(CircleShape)) {
        if (day.withinBudget) {
            Box(Modifier.fillMaxSize().background(colour))
        } else {
            // Over budget: coral stripes rather than an alarming solid block.
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Bright.Coral)
                val gap = 12.dp.toPx()
                var x = -size.height
                while (x < size.width) {
                    drawLine(Color(0xFFFFD3C7), Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 6.dp.toPx())
                    x += gap
                }
            }
        }
    }
}
