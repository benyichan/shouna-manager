package com.shouna.manager.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shouna.manager.data.AppGraph
import com.shouna.manager.data.db.entity.SubZoneEntity
import com.shouna.manager.data.db.entity.ZoneEntity
import com.shouna.manager.reminder.ReminderPermissionHelper
import com.shouna.manager.reminder.ReminderScheduler
import com.shouna.manager.ui.components.AppTopBar
import com.shouna.manager.ui.components.SectionCard
import com.shouna.manager.ui.theme.Bg
import com.shouna.manager.ui.theme.Green
import com.shouna.manager.ui.theme.Primary
import com.shouna.manager.ui.theme.PrimaryLight
import com.shouna.manager.ui.theme.Red
import com.shouna.manager.ui.theme.TextPrimary
import com.shouna.manager.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: (() -> Unit)? = null, onOpenBackup: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val zones by AppGraph.zoneRepository.observeZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val subZones by AppGraph.zoneRepository.observeAllSubZones().collectAsStateWithLifecycle(initialValue = emptyList())
    val hour by AppGraph.settingsRepository.remindHour.collectAsStateWithLifecycle(initialValue = 9)
    val minute by AppGraph.settingsRepository.remindMinute.collectAsStateWithLifecycle(initialValue = 0)

    var showHour by remember { mutableStateOf(false) }
    var showMinute by remember { mutableStateOf(false) }
    var vendorHint by remember { mutableStateOf<String?>(null) }
    var notifOk by remember { mutableStateOf(!ReminderPermissionHelper.needsNotificationPermission(context)) }
    var exactOk by remember { mutableStateOf(ReminderPermissionHelper.canScheduleExact(context)) }
    var batteryOk by remember { mutableStateOf(ReminderPermissionHelper.isIgnoringBatteryOptimizations(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifOk = granted
    }

    // 区域编辑对话框状态
    var zoneDialog by remember { mutableStateOf<ZoneDialogState?>(null) }
    var subDialog by remember { mutableStateOf<SubDialogState?>(null) }
    // 当前正从哪个主区域「进入」子区域管理（底部弹窗）
    var sheetZone by remember { mutableStateOf<ZoneEntity?>(null) }

    LaunchedEffect(Unit) { vendorHint = ReminderPermissionHelper.vendorHint(context) }

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        AppTopBar("设置", onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard("到期提醒") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("每日提醒时间", fontSize = 13.sp, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box {
                            OutlinedButton(onClick = { showHour = true }) { Text(String.format("%02d", hour)) }
                            DropdownMenu(expanded = showHour, onDismissRequest = { showHour = false }) {
                                (0..23).forEach { h ->
                                    DropdownMenuItem(text = { Text(String.format("%02d", h)) }, onClick = {
                                        showHour = false
                                        scope.launch {
                                            AppGraph.settingsRepository.setRemindTime(h, minute)
                                            ReminderScheduler(context).scheduleDaily(h, minute)
                                        }
                                    })
                                }
                            }
                        }
                        Text(":", fontSize = 16.sp)
                        Box {
                            OutlinedButton(onClick = { showMinute = true }) { Text(String.format("%02d", minute)) }
                            DropdownMenu(expanded = showMinute, onDismissRequest = { showMinute = false }) {
                                listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55).forEach { m ->
                                    DropdownMenuItem(text = { Text(String.format("%02d", m)) }, onClick = {
                                        showMinute = false
                                        scope.launch {
                                            AppGraph.settingsRepository.setRemindTime(hour, m)
                                            ReminderScheduler(context).scheduleDaily(hour, m)
                                        }
                                    })
                                }
                            }
                        }
                    }
                    Text("到点会检查未来 7 天内到期及已过期的物品并通知。", fontSize = 12.sp, color = TextSecondary)
                    PermissionRow("通知权限", notifOk, "用于弹到期提醒", "去开启") {
                        if (notifOk) {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                )
                            }
                        } else {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    PermissionRow("精确闹钟", exactOk, "保证到点准时触发", "去允许") {
                        if (!exactOk) ReminderPermissionHelper.openExactAlarmSettings(context)
                    }
                    PermissionRow("电池优化白名单", batteryOk, "防止后台提醒被清理", "去开启") {
                        if (!batteryOk) ReminderPermissionHelper.openBatterySettings(context)
                    }
                    vendorHint?.let {
                        Text(it, fontSize = 12.sp, color = com.shouna.manager.ui.theme.Amber, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            SectionCard("数据备份") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpenBackup() }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("备份与恢复", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("›", fontSize = 20.sp, color = TextSecondary)
                    }
                    Text("每周自动备份到指定目录，也可手动备份/恢复", fontSize = 12.sp, color = TextSecondary)
                }
            }

            SectionCard("区域管理") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("主区域、子区域是两层；存放位置会拼成「主·子」，如「卫生间·台盆」。", fontSize = 12.sp, color = TextSecondary)
                    zones.forEach { zone ->
                        ZoneRow(
                            zone = zone,
                            subCount = subZones.count { it.zoneId == zone.id },
                            onOpen = { sheetZone = zone },
                            onEdit = { zoneDialog = ZoneDialogState(zone.id, zone.code, zone.name) },
                            onDelete = { zoneDialog = ZoneDialogState(zone.id, zone.code, zone.name, delete = true) }
                        )
                    }
                    Button(onClick = { zoneDialog = ZoneDialogState(null, "", "") }, modifier = Modifier.fillMaxWidth()) {
                        Text("＋ 添加主区域")
                    }
                }
            }
        }
    }

    // 主区域对话框
    zoneDialog?.let { d ->
        var code by remember(d) { mutableStateOf(d.code) }
        var name by remember(d) { mutableStateOf(d.name) }
        AlertDialog(
            onDismissRequest = { zoneDialog = null },
            title = { Text(if (d.delete) "删除区域" else if (d.id == null) "添加主区域" else "编辑主区域") },
            text = {
                if (d.delete) {
                    Text("确定删除「${d.name}」吗？该区域下的所有位置关联会被移除，子区域一并删除。")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("编号") }, singleLine = true)
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    zoneDialog = null
                    scope.launch {
                        if (d.delete) {
                            AppGraph.zoneRepository.getZone(d.id!!)?.let { AppGraph.zoneRepository.deleteZone(it) }
                        } else if (d.id == null) {
                            if (name.isNotBlank()) {
                                val codeVal = if (code.isBlank()) "区域${zones.size + 1}" else code
                                AppGraph.zoneRepository.addZone(codeVal, name.trim(), zones.size)
                            }
                        } else {
                            AppGraph.zoneRepository.getZone(d.id)?.let {
                                AppGraph.zoneRepository.updateZone(it.copy(code = code.ifBlank { it.code }, name = name.ifBlank { it.name }))
                            }
                        }
                    }
                }) { Text(if (d.delete) "删除" else "保存", color = if (d.delete) Red else Primary) }
            },
            dismissButton = { TextButton(onClick = { zoneDialog = null }) { Text("取消") } }
        )
    }

    // 子区域对话框
    subDialog?.let { d ->
        var name by remember(d) { mutableStateOf(d.name) }
        AlertDialog(
            onDismissRequest = { subDialog = null },
            title = { Text(if (d.sub == null) "添加子区域" else "编辑子区域") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称，如：冰箱冷冻层") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    subDialog = null
                    scope.launch {
                        if (name.isNotBlank()) {
                            if (d.sub == null) {
                                AppGraph.zoneRepository.addSubZone(d.zoneId, name.trim())
                            } else {
                                AppGraph.zoneRepository.updateSubZone(d.sub.copy(name = name.trim()))
                            }
                        }
                    }
                }) { Text("保存", color = Primary) }
            },
            dismissButton = { TextButton(onClick = { subDialog = null }) { Text("取消") } }
        )
    }

    // 子区域管理底部弹窗：从主区域「进入」
    sheetZone?.let { zoneInfo ->
        val subs = subZones.filter { it.zoneId == zoneInfo.id }
        ModalBottomSheet(
            onDismissRequest = { sheetZone = null },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${zoneInfo.name} · 子区域", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { subDialog = SubDialogState(zoneInfo.id, null, "") }) { Text("＋ 添加", color = Primary) }
                }
                Spacer(Modifier.height(8.dp))
                if (subs.isEmpty()) {
                    Text("暂无子区域，点右上角添加。", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    subs.forEach { sub ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("· ${sub.name}", fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            TextButton(onClick = { subDialog = SubDialogState(zoneInfo.id, sub, sub.name) }) { Text("改", fontSize = 13.sp) }
                            TextButton(onClick = { scope.launch { AppGraph.zoneRepository.deleteSubZone(sub) } }) { Text("删", fontSize = 13.sp, color = Red) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("子区域会与主区域拼成「主·子」作为存放位置。", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

private data class ZoneDialogState(val id: Long?, val code: String, val name: String, val delete: Boolean = false)
private data class SubDialogState(val zoneId: Long, val sub: SubZoneEntity?, val name: String)

@Composable
private fun PermissionRow(title: String, enabled: Boolean, desc: String, actionText: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(desc, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        Text(
            text = if (enabled) "已开启" else actionText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Green else Primary
        )
    }
}

@Composable
private fun ZoneRow(
    zone: ZoneEntity,
    subCount: Int,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5EFE8), RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onOpen() }
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 8.dp).weight(1f)
        )
        Text(
            text = "$subCount 个子区域",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Text(
            text = "›",
            fontSize = 18.sp,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        TextButton(onClick = onEdit) { Text("编辑", fontSize = 12.sp) }
        TextButton(onClick = onDelete) { Text("删除", fontSize = 12.sp, color = Red) }
    }
}
