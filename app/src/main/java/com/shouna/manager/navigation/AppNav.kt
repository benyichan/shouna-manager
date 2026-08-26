package com.shouna.manager.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.shouna.manager.ui.screens.BackupScreen
import com.shouna.manager.ui.screens.ExpiringScreen
import com.shouna.manager.ui.screens.ItemDetailScreen
import com.shouna.manager.ui.screens.ItemEditScreen
import com.shouna.manager.ui.screens.ItemsScreen
import com.shouna.manager.ui.screens.SettingsScreen
import com.shouna.manager.ui.screens.ZoneItemsScreen
import com.shouna.manager.ui.screens.ZonesOverviewScreen
import com.shouna.manager.ui.theme.Bg
import com.shouna.manager.ui.theme.Primary
import com.shouna.manager.ui.theme.TextSecondary

sealed interface Route {
    data object Items : Route
    data object Zones : Route
    data object Expiring : Route
    data object Settings : Route
    data class ItemEdit(val itemId: Long? = null) : Route
    data class ItemDetail(val itemId: Long) : Route
    data class ZoneItems(val zoneId: Long, val zoneName: String) : Route
    data object Backup : Route
}

private data class TabDef(val route: Route, val label: String, val icon: ImageVector, val activeIcon: ImageVector)

private val TABS = listOf(
    TabDef(Route.Items, "物品", Icons.Outlined.Home, Icons.Filled.Home),
    TabDef(Route.Zones, "区域", Icons.Outlined.Place, Icons.Filled.Place),
    TabDef(Route.Expiring, "临期", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    TabDef(Route.Settings, "设置", Icons.Outlined.Settings, Icons.Filled.Settings)
)

@Composable
fun AppNav(openExpiring: Boolean = false) {
    val initialRoute: Route = if (openExpiring) Route.Expiring else Route.Items
    val backStack = remember { mutableStateListOf<Any>(initialRoute) }
    val top = backStack.lastOrNull()
    val isTopLevel = TABS.any { it.route == top }

    fun switchTab(route: Route) {
        backStack.clear()
        backStack.add(route)
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            transitionSpec = { fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(280)) },
            popTransitionSpec = { fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(280)) },
            predictivePopTransitionSpec = { fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(280)) },
            entryProvider = { key ->
                when (key) {
                    Route.Items -> NavEntry(key) {
                        ItemsScreen(
                            onAdd = { backStack.add(Route.ItemEdit(null)) },
                            onOpenItem = { id -> backStack.add(Route.ItemDetail(id)) }
                        )
                    }
                    Route.Zones -> NavEntry(key) {
                        ZonesOverviewScreen(
                            onOpenZone = { id, name -> backStack.add(Route.ZoneItems(id, name)) }
                        )
                    }
                    Route.Expiring -> NavEntry(key) {
                        ExpiringScreen(
                            onOpenItem = { id -> backStack.add(Route.ItemDetail(id)) }
                        )
                    }
                    Route.Settings -> NavEntry(key) {
                        SettingsScreen(
                            onOpenBackup = { backStack.add(Route.Backup) }
                        )
                    }
                    is Route.ItemEdit -> NavEntry(key) {
                        ItemEditScreen(itemId = key.itemId, onBack = { backStack.removeLastOrNull() })
                    }
                    is Route.ItemDetail -> NavEntry(key) {
                        ItemDetailScreen(
                            itemId = key.itemId,
                            onBack = { backStack.removeLastOrNull() },
                            onEdit = { id -> backStack.add(Route.ItemEdit(id)) },
                            onDeleted = { backStack.removeLastOrNull() },
                            onOpenItem = { id -> backStack.add(Route.ItemDetail(id)) }
                        )
                    }
                    is Route.ZoneItems -> NavEntry(key) {
                        ZoneItemsScreen(
                            zoneId = key.zoneId,
                            zoneName = key.zoneName,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenItem = { id -> backStack.add(Route.ItemDetail(id)) }
                        )
                    }
                    Route.Backup -> NavEntry(key) {
                        BackupScreen(onBack = { backStack.removeLastOrNull() })
                    }
                    else -> NavEntry(key) { Text("未实现页面") }
                }
            }
        )

        if (isTopLevel) {
            BottomTabBar(
                current = top as? Route,
                onSelect = ::switchTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Bg)
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun BottomTabBar(current: Route?, onSelect: (Route) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .navigationBarsPadding()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        TABS.forEach { tab ->
            val active = tab.route == current
            val tint = if (active) Primary else TextSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(tab.route) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (active) tab.activeIcon else tab.icon,
                    contentDescription = tab.label,
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = tab.label,
                    fontSize = 11.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = tint,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
