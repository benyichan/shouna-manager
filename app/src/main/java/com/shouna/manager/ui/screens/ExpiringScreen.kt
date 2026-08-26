package com.shouna.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CheckCircle
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
import com.shouna.manager.domain.ExpiryCalculator
import com.shouna.manager.domain.ExpiryStatus
import com.shouna.manager.domain.WasteAnalysis
import com.shouna.manager.ui.Format
import com.shouna.manager.ui.components.AppTopBar
import com.shouna.manager.ui.components.EmptyState
import com.shouna.manager.ui.components.ItemRow
import com.shouna.manager.ui.locationLabel
import com.shouna.manager.ui.theme.Amber
import com.shouna.manager.ui.theme.Bg
import com.shouna.manager.ui.theme.Red
import com.shouna.manager.ui.theme.TextSecondary

@Composable
fun ExpiringScreen(onBack: (() -> Unit)? = null, onOpenItem: (Long) -> Unit) {
    val items by AppGraph.itemRepository.observeAllItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val zones by AppGraph.zoneRepository.observeZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val subZones by AppGraph.zoneRepository.observeAllSubZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val itemZones by AppGraph.itemRepository.observeAllItemZones().collectAsStateWithLifecycle(initialValue = emptyList())

    val zoneNameById = zones.associate { it.id to it.name }
    val subZoneNameById = subZones.associate { it.id to it.name }
    val zonesByItem = itemZones.groupBy { it.itemId }

    val expired = items.filter { ExpiryCalculator.statusOf(it) == ExpiryStatus.EXPIRED }
    val expiring = items.filter { ExpiryCalculator.statusOf(it) == ExpiryStatus.EXPIRING }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        AppTopBar("临期清单", onBack)
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.AddCircle,
                    title = "还没有物品",
                    hint = "先到「物品」页添加，到期前会在这里提醒"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (expired.isNotEmpty()) {
                    item { SectionTitle("已过期", expired.size, Red) }
                    items(expired, key = { it.id }) { item ->
                        ItemRow(item, locationLabel(item.id, zonesByItem, zoneNameById, subZoneNameById)) { onOpenItem(item.id) }
                    }
                }
                if (expiring.isNotEmpty()) {
                    item { SectionTitle("即将到期", expiring.size, Amber) }
                    items(expiring, key = { it.id }) { item ->
                        ItemRow(item, locationLabel(item.id, zonesByItem, zoneNameById, subZoneNameById)) { onOpenItem(item.id) }
                    }
                }
                if (expired.isEmpty() && expiring.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.TopCenter) {
                            EmptyState(
                                icon = Icons.Outlined.CheckCircle,
                                title = "暂无临期或过期物品",
                                hint = "一切都在保质期内"
                            )
                        }
                    }
                }
                // 浪费分析（近 3 个月已处理的过期物品）
                item {
                    val hasWaste = WasteAnalysis.hasAny(items)
                    val wasteStats = if (hasWaste) WasteAnalysis.monthly(items) else emptyList()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Text("浪费分析（近 3 个月）", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Amber)
                        Spacer(Modifier.height(8.dp))
                        if (hasWaste) {
                            wasteStats.forEach { m ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(m.yearMonth, fontSize = 13.sp, color = TextSecondary)
                                    Text(
                                        text = buildString {
                                            append("${m.count} 件")
                                            if (m.amountSum > 0) append(" · ${Format.amount(m.amountSum)}")
                                            m.topTag?.let { append(" · 多为「$it」") }
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = com.shouna.manager.ui.theme.TextPrimary
                                    )
                                }
                            }
                        } else {
                            Text(
                                "近 3 个月还没有浪费记录。把过期后处理掉的物品标记为已处理，会统计在这里。",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        Text("口径：处理时已过期的物品", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int, color: Color) {
    Text(
        text = "$title（$count）",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}
