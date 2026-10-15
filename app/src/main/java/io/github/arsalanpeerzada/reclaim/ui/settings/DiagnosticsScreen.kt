package io.github.arsalanpeerzada.reclaim.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arsalanpeerzada.reclaim.R
import io.github.arsalanpeerzada.reclaim.counting.CountingDiagnostics
import io.github.arsalanpeerzada.reclaim.counting.Decision
import io.github.arsalanpeerzada.reclaim.counting.TrackedApp
import io.github.arsalanpeerzada.reclaim.ui.components.BodyText
import io.github.arsalanpeerzada.reclaim.ui.components.CircleIconButton
import io.github.arsalanpeerzada.reclaim.ui.components.ScreenTitle
import io.github.arsalanpeerzada.reclaim.ui.components.TextLink
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(diagnostics: CountingDiagnostics, onBack: () -> Unit) {
    val palette = ReclaimTheme.palette
    val entries by diagnostics.entries.collectAsStateWithLifecycle()
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(R.drawable.ic_back, "Back", onBack)
            ScreenTitle("Diagnostics", Modifier.weight(1f).padding(start = 14.dp))
            TextLink("Clear", diagnostics::clear)
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                BodyText(
                    "Newest first. Swipe through a few reels, then come back: each new reel should show Counted once, and " +
                        "scrolling the feed, stories or grids should not. Kept in memory only and cleared when the app closes.",
                )
            }
            if (entries.isEmpty()) {
                item { BodyText("Nothing yet. Is counting switched on?", Modifier.padding(vertical = 16.dp)) }
            }
            items(entries) { entry ->
                val s = entry.signal
                val counted = entry.decision.reels > 0
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(if (counted) palette.softMint else palette.card, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "${time.format(Date(entry.atMillis))}  ${TrackedApp.fromPackage(s.packageName)?.displayName ?: s.packageName}  " +
                            entry.decision.description,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.text,
                    )
                    val heightPercent = if (s.screenHeight > 0 && s.containerHeight > 0) s.containerHeight * 100 / s.screenHeight else -1
                    Text(
                        "position ${s.fromIndex}..${s.toIndex} of ${s.itemCount}  height $heightPercent%  rows ${s.rowCount} cols ${s.columnCount}\n" +
                            "id ${s.viewId ?: "none"}\nclass ${s.className ?: "none"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = palette.textMuted,
                    )
                }
            }
        }
    }
}
