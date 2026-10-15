package io.github.arsalanpeerzada.reclaim.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            // The selected tab grows into a sunflower pill with its label; the others shrink back to icons.
            val background by animateColorAsState(if (item.selected) Bright.Sunflower else Color.Transparent, tween(250), label = "tab")
            val tint by animateColorAsState(if (item.selected) Bright.Ink else palette.onPill, tween(250), label = "tab icon")
            Row(
                Modifier
                    .height(48.dp)
                    .selectable(selected = item.selected, onClick = item.onClick, role = Role.Tab)
                    .background(background, CircleShape)
                    .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(painterResource(item.icon), contentDescription = item.label, tint = tint, modifier = Modifier.size(22.dp))
                AnimatedVisibility(visible = item.selected, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                    Text(item.label, style = MaterialTheme.typography.labelLarge, color = Bright.Ink, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
