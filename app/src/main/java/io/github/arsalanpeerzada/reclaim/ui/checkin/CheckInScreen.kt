package io.github.arsalanpeerzada.reclaim.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import io.github.arsalanpeerzada.reclaim.AppContainer
import io.github.arsalanpeerzada.reclaim.R
import io.github.arsalanpeerzada.reclaim.domain.Who5
import io.github.arsalanpeerzada.reclaim.ui.components.BodyText
import io.github.arsalanpeerzada.reclaim.ui.components.CircleIconButton
import io.github.arsalanpeerzada.reclaim.ui.components.GameCard
import io.github.arsalanpeerzada.reclaim.ui.components.PillButton
import io.github.arsalanpeerzada.reclaim.ui.components.PipMood
import io.github.arsalanpeerzada.reclaim.ui.components.PipSays
import io.github.arsalanpeerzada.reclaim.ui.components.ScreenTitle
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

/** The monthly WHO-5 check-in: five statements about the last two weeks, then the score and its history. */
@Composable
fun CheckInScreen(container: AppContainer, onBack: () -> Unit) {
    val palette = ReclaimTheme.palette
    val scope = rememberCoroutineScope()
    val history by container.wellbeing.who5.collectAsState(initial = emptyMap())
    // One answer per statement: its score, or -1 while unanswered.
    var answers by rememberSaveable { mutableStateOf(List(Who5.statements.size) { -1 }) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CircleIconButton(R.drawable.ic_back, "Back", onBack)
            ScreenTitle("Check-in")
        }
        if (!submitted) {
            BodyText(
                "Five statements about the last two weeks. Pick how often each was true. " +
                    "This is the WHO-5 Wellbeing Index; your answers stay on this phone.",
            )
            Who5.statements.forEachIndexed { index, statement ->
                GameCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                    Text("${index + 1}. $statement", style = MaterialTheme.typography.titleMedium, color = palette.text)
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Who5.answers.forEach { (label, score) ->
                            AnswerRow(label, selected = answers[index] == score, colour = Bright.cycle[index % Bright.cycle.size]) {
                                answers = answers.toMutableList().also { it[index] = score }
                            }
                        }
                    }
                }
            }
            Text(
                "WHO-5 Well-Being Index, WHO Regional Office for Europe (1998). Free to use. " +
                    "Scoring and the 50 cut-off follow Topp et al., Psychother Psychosom 2015;84:167-176.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
            )
            PillButton(
                "See my score",
                {
                    scope.launch { container.wellbeing.saveWho5(answers.sum()) }
                    submitted = true
                },
                badge = Bright.Mint,
                enabled = answers.none { it < 0 },
            )
        } else {
            Result(raw = answers.sum(), history = history, today = container.reels.today(), onDone = onBack)
        }
    }
}

@Composable
private fun AnswerRow(label: String, selected: Boolean, colour: Color, onClick: () -> Unit) {
    val palette = ReclaimTheme.palette
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(if (selected) colour else palette.ground, RoundedCornerShape(16.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(18.dp).background(if (selected) Bright.Ink else palette.track, CircleShape),
        )
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (selected) Bright.Ink else palette.text)
    }
}

@Composable
private fun Result(raw: Int, history: Map<LocalDate, Int>, today: LocalDate, onDone: () -> Unit) {
    val palette = ReclaimTheme.palette
    val percent = Who5.percent(raw)
    // Today's result replaces any earlier one today; compare with the last one before today.
    val ordered = (history + (today to raw)).toSortedMap()
    val previous = ordered.entries.lastOrNull { it.key.isBefore(today) }

    GameCard(Modifier.fillMaxWidth(), colour = palette.softViolet) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$percent", style = MaterialTheme.typography.displayLarge, color = palette.text)
            Text(" / 100", style = MaterialTheme.typography.titleLarge, color = palette.textMuted, modifier = Modifier.padding(bottom = 10.dp))
        }
        val change = previous?.let { percent - Who5.percent(it.value) }
        PipSays(
            if (percent > Who5.LOW_PERCENT) PipMood.Happy else PipMood.Calm,
            when {
                change == null -> "Your first check-in"
                change > 0 -> "Up $change since last time"
                change < 0 -> "Down ${-change} since last time"
                else -> "Same as last time"
            },
            "Higher means better wellbeing. Next check-in in about a month.",
        )
    }
    if (percent <= Who5.LOW_PERCENT) {
        GameCard(Modifier.fillMaxWidth(), colour = palette.softSky, padding = 16.dp) {
            Text("Look after yourself", style = MaterialTheme.typography.titleLarge, color = palette.text)
            Text(
                "This score is on the low side. It isn't a diagnosis, but if you've been feeling low for a while, " +
                    "talking to someone you trust or your GP can really help.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text,
            )
        }
    }
    if (ordered.size > 1) {
        GameCard(Modifier.fillMaxWidth(), padding = 16.dp) {
            Text("Your check-ins", style = MaterialTheme.typography.titleLarge, color = palette.text)
            ordered.entries.toList().takeLast(6).forEach { (date, score) ->
                val p = Who5.percent(score)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(date.format(MonthDay), style = MaterialTheme.typography.labelMedium, color = palette.textMuted, modifier = Modifier.weight(0.3f))
                    Box(Modifier.weight(0.6f).height(14.dp).background(palette.track, CircleShape)) {
                        Box(Modifier.fillMaxWidth(p / 100f).height(14.dp).background(Bright.Violet, CircleShape))
                    }
                    Text("$p", style = MaterialTheme.typography.labelMedium, color = palette.text, modifier = Modifier.weight(0.1f))
                }
            }
        }
    }
    PillButton("Done", onDone, badge = Bright.Mint)
}

private val MonthDay = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
