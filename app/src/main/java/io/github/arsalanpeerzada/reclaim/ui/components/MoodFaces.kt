package io.github.arsalanpeerzada.reclaim.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.arsalanpeerzada.reclaim.domain.Moods
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme

private val MoodColours = listOf(Bright.Sky, Bright.Violet, Bright.Pink, Bright.Sunflower, Bright.Mint)

/** Five faces, low to great. [selected] is 1 to 5, or null when today has no tag yet. */
@Composable
fun MoodFaces(selected: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
        Moods.labels.forEachIndexed { index, label ->
            val mood = index + 1
            val isSelected = selected == mood
            Column(
                Modifier
                    .selectable(selected = isSelected, onClick = { onSelect(mood) }, role = Role.RadioButton)
                    .alpha(if (selected == null || isSelected) 1f else 0.45f)
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Face(
                    colour = MoodColours[index],
                    smile = (index - 2) / 2f,
                    modifier = Modifier
                        .size(48.dp)
                        .then(if (isSelected) Modifier.border(3.dp, ReclaimTheme.palette.text, CircleShape) else Modifier),
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = ReclaimTheme.palette.text, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** A round face; [smile] from -1 (mouth turned down) to 1 (big smile). */
@Composable
private fun Face(colour: Color, smile: Float, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(colour, radius = r * 0.9f, center = c)
        drawCircle(Bright.Ink, radius = r * 0.09f, center = Offset(c.x - r * 0.3f, c.y - r * 0.15f))
        drawCircle(Bright.Ink, radius = r * 0.09f, center = Offset(c.x + r * 0.3f, c.y - r * 0.15f))
        val mouthY = c.y + r * 0.3f
        val mouth = Path().apply {
            moveTo(c.x - r * 0.32f, mouthY)
            quadraticTo(c.x, mouthY + smile * r * 0.35f, c.x + r * 0.32f, mouthY)
        }
        drawPath(mouth, Bright.Ink, style = Stroke(width = r * 0.11f, cap = StrokeCap.Round))
    }
}
