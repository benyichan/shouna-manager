package com.shouna.manager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shouna.manager.data.AppGraph
import com.shouna.manager.data.PhotoStorage
import com.shouna.manager.domain.ExpiryCalculator
import com.shouna.manager.domain.ExpiryStatus
import com.shouna.manager.domain.SameDate
import com.shouna.manager.domain.TagUtils
import com.shouna.manager.ui.Format
import com.shouna.manager.ui.components.AppTopBar
import com.shouna.manager.ui.components.DetailRow
import com.shouna.manager.ui.components.LocationTag
import com.shouna.manager.ui.components.SectionCard
import com.shouna.manager.ui.components.StatusBadge
import com.shouna.manager.ui.locationLabel
import com.shouna.manager.ui.theme.Bg
import com.shouna.manager.ui.theme.Primary
import com.shouna.manager.ui.theme.Red
import com.shouna.manager.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ItemDetailScreen(itemId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit, onDeleted: () -> Unit, onOpenItem: (Long) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val item by AppGraph.itemRepository.observeItem(itemId).collectAsStateWithLifecycle(initialValue = null)
    val photos by AppGraph.itemRepository.observePhotos(itemId).collectAsStateWithLifecycle(initialValue = emptyList())
    val zones by AppGraph.zoneRepository.observeZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val subZones by AppGraph.zoneRepository.observeAllSubZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val itemZones by AppGraph.itemRepository.observeAllItemZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val allItems by AppGraph.itemRepository.observeAllItems().collectAsStateWithLifecycle(initialValue = emptyList())

    var showHandleMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val current = item
    if (current == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Bg),
            contentAlignment = Alignment.Center
        ) { Text("加载中…", color = TextSecondary) }
        return
    }
    val status = ExpiryCalculator.statusOf(current)
    val zoneNameById = zones.associate { it.id to it.name }
    val subZoneNameById = subZones.associate { it.id to it.name }
    val loc = locationLabel(current.id, itemZones.groupBy { it.itemId }, zoneNameById, subZoneNameById)
    // 「往年今日」只收往年：今年同日购买的（含今天刚买的）不算
    val pastToday = allItems
        .filter { it.id != current.id && SameDate.isSameMonthDay(it.purchaseDate, current.purchaseDate) }
        .filter { SameDate.year(it.purchaseDate!!) < SameDate.year(current.purchaseDate!!) }
        .sortedByDescending { it.purchaseDate ?: 0L }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar("物品详情", onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 头部：名称 + 状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(current.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    StatusBadge(status)
                }

                if (photos.isNotEmpty()) {
                    SectionCard("照片") {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            photos.forEach { p ->
                                // 缩略图按 path 记忆，避免每次重组都重复解码大图
                                val bmp = remember(p.path) { PhotoStorage.decodeThumb(PhotoStorage.loadFile(context, p.path), 300) }
                                if (bmp != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(120.dp)
                                            .background(Color(0xFFF0EAE3), RoundedCornerShape(10.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                SectionCard("基本信息") {
                    Column {
                        val tags = TagUtils.split(current.tags)
                        if (tags.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tags.forEach { LocationTag(it) }
                            }
                        }
                        DetailRow("状态", Format.expiryHint(current.expiryDate, current.handled))
                        current.specModel?.let { DetailRow("规格 / 型号", it) }
                        current.purchaseDate?.let { DetailRow("购买日期", Format.date(it)) }
                        current.platform?.let { DetailRow("购买平台", it) }
                        current.amount?.let { DetailRow("购买金额", Format.amount(it)) }
                        DetailRow("购买数量", "${current.quantity}")
                        current.productionDate?.let { DetailRow("生产日期", Format.date(it)) }
                        current.shelfLifeText?.let { DetailRow("保质期", it) }
                        current.expiryDate?.let { DetailRow("有效日期", Format.date(it)) }
                        current.note.ifBlank { null }?.let { DetailRow("备注", it) }
                    }
                }

                SectionCard("存放位置") {
                    Text(
                        text = if (loc.isBlank()) "未设置位置" else loc,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                SectionCard("往年今日购买") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (pastToday.isEmpty()) {
                            Text(
                                text = "往年今日暂无购买记录",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            pastToday.forEach { other ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenItem(other.id) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${SameDate.year(other.purchaseDate ?: 0L)}  ${Format.date(other.purchaseDate)}",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(other.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!current.handled) {
                        Box(modifier = Modifier.weight(1f)) {
                            Button(onClick = { showHandleMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("处理")
                            }
                            DropdownMenu(expanded = showHandleMenu, onDismissRequest = { showHandleMenu = false }) {
                                listOf("已食用", "已丢弃", "已转移").forEach { t ->
                                    DropdownMenuItem(text = { Text(t) }, onClick = {
                                        showHandleMenu = false
                                        scope.launch {
                                            AppGraph.itemRepository.markHandled(current, t)
                                            onBack()
                                        }
                                    })
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch { AppGraph.itemRepository.unhandle(current) }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("撤销处理") }
                    }
                    OutlinedButton(onClick = { onEdit(current.id) }, modifier = Modifier.weight(1f)) {
                        Text("编辑")
                    }
                }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) { Text("删除物品", color = Red) }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除物品") },
            text = { Text("确定删除「${current.name}」吗？照片也会一并删除。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        AppGraph.itemRepository.delete(current)
                        onDeleted()
                    }
                }) { Text("删除", color = Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}
