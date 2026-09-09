package com.shouna.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shouna.manager.data.AppGraph
import com.shouna.manager.data.db.entity.ZoneEntity
import com.shouna.manager.domain.ExpiryCalculator
import com.shouna.manager.domain.ExpiryStatus
import com.shouna.manager.ui.components.EmptyState
import com.shouna.manager.ui.theme.Bg
import com.shouna.manager.ui.theme.Primary
import com.shouna.manager.ui.theme.PrimaryLight
import com.shouna.manager.ui.theme.Red
import com.shouna.manager.ui.theme.TextPrimary
import com.shouna.manager.ui.theme.TextSecondary

@Composable
fun ZonesOverviewScreen(onOpenZone: (Long, String) -> Unit) {
    val zones by AppGraph.zoneRepository.observeZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val items by AppGraph.itemRepository.observeAllItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val itemZones by AppGraph.itemRepository.observeAllItemZones().collectAsStateWithLifecycle(initialValue = emptyList())

    // zoneId -> set of itemId
    val itemIdsByZone = itemZones.groupBy({ it.zoneId }, { it.itemId }).mapValues { it.value.toSet() }

    Column(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
            Text("区域分布", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        if (zones.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.Place,
                    title = "还没有区域",
                    hint = "到「设置 → 区域管理」添加主区域"
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(zones, key = { it.id }) { zone ->
                    ZoneCell(
                        zone = zone,
                        itemCount = itemIdsByZone[zone.id]?.size ?: 0,
                        expiringCount = items.count { item ->
                            val st = ExpiryCalculator.statusOf(item)
                            (st == ExpiryStatus.EXPIRING || st == ExpiryStatus.EXPIRED) &&
                                (itemIdsByZone[zone.id]?.contains(item.id) ?: false)
                        },
                        onClick = { onOpenZone(zone.id, zone.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneCell(zone: ZoneEntity, itemCount: Int, expiringCount: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = zone.code,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                modifier = Modifier
                    .background(PrimaryLight, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Text(
                text = zone.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            text = "$itemCount 件物品",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (expiringCount > 0) {
            Text(
                text = "$expiringCount 件临期/过期",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Red,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
