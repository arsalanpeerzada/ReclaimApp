package io.github.arsalanpeerzada.reclaim.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.arsalanpeerzada.reclaim.ui.theme.Bright
import io.github.arsalanpeerzada.reclaim.ui.theme.ReclaimTheme

data class NavItem(val label: String, @DrawableRes val icon: Int, val selected: Boolean, val onClick: () -> Unit)

/** Space for screens to leave at the bottom so content can scroll clear of the bar. */
val FloatingNavClearance = 108.dp

/** The floating pill navigation bar. The selected item opens into a sunflower pill with its label. */
@Composable
fun FloatingNav(items: List<NavItem>, modifier: Modifier = Modifier) {
    val palette = ReclaimTheme.palette
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .fillMaxWidth()
            .height(68.dp)
            .background(palette.pill, CircleShape)
            .padding(horizontal = 10.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val itemModifier = Modifier
                .height(48.dp)
                .selectable(selected = item.selected, onClick = item.onClick, role = Role.Tab)
            if (item.selected) {
                Row(
                    itemModifier.background(Bright.Sunflower, CircleShape).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(painterResource(item.icon), contentDescription = null, tint = Bright.Ink, modifier = Modifier.size(20.dp))
                    Text(item.label, style = MaterialTheme.typography.labelLarge, color = Bright.Ink)
                }
            } else {
                Box(itemModifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(item.icon), contentDescription = item.label, tint = palette.onPill, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
