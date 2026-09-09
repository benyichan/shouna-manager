package com.shouna.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shouna.manager.data.AppGraph
import com.shouna.manager.domain.ExpiryCalculator
import com.shouna.manager.domain.ExpiryStatus
import com.shouna.manager.domain.TagUtils
import com.shouna.manager.ui.components.EmptyState
import com.shouna.manager.ui.components.ItemRow
import com.shouna.manager.ui.components.StatusBadge
import com.shouna.manager.ui.locationLabel
import com.shouna.manager.ui.theme.Bg
import com.shouna.manager.ui.theme.Primary
import com.shouna.manager.ui.theme.TextPrimary
import com.shouna.manager.ui.theme.TextSecondary

@Composable
fun ItemsScreen(onAdd: () -> Unit, onOpenItem: (Long) -> Unit) {
    val items by AppGraph.itemRepository.observeAllItems().collectAsStateWithLifecycle(initialValue = emptyList())
    val zones by AppGraph.zoneRepository.observeZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val subZones by AppGraph.zoneRepository.observeAllSubZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val itemZones by AppGraph.itemRepository.observeAllItemZones().collectAsStateWithLifecycle(initialValue = emptyList())

    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<ExpiryStatus?>(null) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var restockOnly by remember { mutableStateOf(false) }

    val zoneNameById = zones.associate { it.id to it.name }
    val subZoneNameById = subZones.associate { it.id to it.name }
    val zonesByItem = itemZones.groupBy { it.itemId }
    val allTags = items.flatMap { TagUtils.split(it.tags) }.distinct()

    val filtered = items.filter { item ->
        val matchQ = query.isBlank() || item.name.contains(query.trim(), ignoreCase = true)
        val st = ExpiryCalculator.statusOf(item)
        val matchS = statusFilter == null || st == statusFilter
        val matchTag = tagFilter == null || TagUtils.split(item.tags).contains(tagFilter)
        val matchR = !restockOnly || (item.isConsumable && item.quantity <= item.restockThreshold)
        matchQ && matchS && matchTag && matchR
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("物品", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            SearchBar(query, { query = it }, Modifier.padding(horizontal = 20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip("全部", statusFilter == null && !restockOnly) {
                    statusFilter = null; restockOnly = false
                }
                FilterChip("临期", statusFilter == ExpiryStatus.EXPIRING) { statusFilter = ExpiryStatus.EXPIRING }
                FilterChip("已过期", statusFilter == ExpiryStatus.EXPIRED) { statusFilter = ExpiryStatus.EXPIRED }
                FilterChip("已处理", statusFilter == ExpiryStatus.HANDLED) { statusFilter = ExpiryStatus.HANDLED }
                FilterChip("需补货", restockOnly) { restockOnly = !restockOnly }
            }
            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip("全部标签", tagFilter == null) { tagFilter = null }
                    allTags.forEach { tag ->
                        FilterChip(tag, tagFilter == tag) { tagFilter = if (tagFilter == tag) null else tag }
                    }
                }
            }
            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Outlined.AddCircle,
                        title = "还没有物品",
                        hint = "点右下角按钮，添加第一件物品"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            locationLabel = locationLabel(item.id, zonesByItem, zoneNameById, subZoneNameById),
                            onClick = { onOpenItem(item.id) }
                        )
                    }
                }
            }
        }

        androidx.compose.material3.FloatingActionButton(
            onClick = onAdd,
            containerColor = Primary,
            contentColor = Color.White,
            // 底部 TabBar 高度 = 系统导航栏 inset + ~63dp 自身内容，FAB 先让过 inset 再留 88dp，任何导航模式下都不被压住
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 88.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "添加物品")
        }
    }
}

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = TextPrimary)
            )
            if (value.isEmpty()) {
                Text("搜索物品名称…", fontSize = 14.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) com.shouna.manager.ui.theme.Primary else Color.White
    val fg = if (selected) Color.White else TextSecondary
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = fg)
    }
}
