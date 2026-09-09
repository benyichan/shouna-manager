package com.shouna.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shouna.manager.data.AppGraph
import com.shouna.manager.ui.components.AppTopBar
import com.shouna.manager.ui.components.EmptyState
import com.shouna.manager.ui.components.ItemRow
import com.shouna.manager.ui.locationLabel
import com.shouna.manager.ui.theme.Bg
import com.shouna.manager.ui.theme.TextSecondary

@Composable
fun ZoneItemsScreen(zoneId: Long, zoneName: String, onBack: () -> Unit, onOpenItem: (Long) -> Unit) {
    val items by AppGraph.itemRepository.observeAllItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val zones by AppGraph.zoneRepository.observeZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val subZones by AppGraph.zoneRepository.observeAllSubZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val itemZones by AppGraph.itemRepository.observeAllItemZones().collectAsStateWithLifecycle(initialValue = emptyList())

    val zoneNameById = zones.associate { it.id to it.name }
    val subZoneNameById = subZones.associate { it.id to it.name }
    val zonesByItem = itemZones.groupBy { it.itemId }
    val itemIdsInZone = itemZones.filter { it.zoneId == zoneId }.map { it.itemId }.toSet()
    val filtered = items.filter { it.id in itemIdsInZone }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        AppTopBar(zoneName, onBack)
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.Place,
                    title = "该区域暂无物品",
                    hint = "添加或编辑物品时，把存放位置选到这里"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    ItemRow(item, locationLabel(item.id, zonesByItem, zoneNameById, subZoneNameById)) { onOpenItem(item.id) }
                }
            }
        }
    }
}
