package com.shouna.manager.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shouna.manager.backup.BackupManager
import com.shouna.manager.data.AppGraph
import com.shouna.manager.ui.Format
import com.shouna.manager.ui.components.AppTopBar
import com.shouna.manager.ui.components.SectionCard
import com.shouna.manager.ui.theme.Bg
import com.shouna.manager.ui.theme.Green
import com.shouna.manager.ui.theme.Primary
import com.shouna.manager.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupManager = remember { BackupManager(context.applicationContext) }

    var backupDir by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    val lastBackupAt by AppGraph.settingsRepository.lastBackupAt.collectAsStateWithLifecycle(initialValue = 0L)

    LaunchedEffect(Unit) { backupDir = AppGraph.settingsRepository.getBackupDirUri() }

    val dirLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            backupDir = uri.toString()
            scope.launch { AppGraph.settingsRepository.setBackupDirUri(uri.toString()) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            restoreUri = uri
            confirmRestore = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
    ) {
        AppTopBar("备份与恢复", onBack)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard("备份目录") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (backupDir != null) "已设置备份目录" else "未设置备份目录",
                        fontSize = 14.sp,
                        fontWeight = if (backupDir != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (backupDir != null) Green else TextSecondary
                    )
                    Text(
                        text = "App 每周自动把备份保存到该目录（只保留最近 7 份）。建议选择网盘同步文件夹（如坚果云），可顺带同步到云端。",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedButton(onClick = { dirLauncher.launch(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (backupDir == null) "选择备份目录" else "更换备份目录")
                    }
                }
            }

            SectionCard("手动备份 / 恢复") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        scope.launch {
                            val dir = backupDir
                            if (dir == null) {
                                message = "请先选择备份目录"
                            } else {
                                message = runCatching {
                                    val s = backupManager.export(Uri.parse(dir))
                                    AppGraph.settingsRepository.setLastBackupAt(System.currentTimeMillis())
                                    backupManager.cleanupOld(Uri.parse(dir), 7)
                                    "备份完成：${s.items} 件物品、${s.zones} 个区域、${s.photos} 张照片"
                                }.getOrElse { "备份失败：${it.message}" }
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("立即备份")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                        Text("从备份恢复")
                    }
                    Text(
                        text = if (lastBackupAt > 0) "上次备份：${Format.date(lastBackupAt)}" else "尚无备份记录",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            message?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = if (it.startsWith("备份完成") || it.startsWith("恢复完成")) Green else Color(0xFFD64545),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (confirmRestore && restoreUri != null) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("确认恢复") },
            text = { Text("恢复将覆盖当前所有数据，且应用会重启。继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    scope.launch {
                        message = runCatching {
                            val s = backupManager.restore(restoreUri!!)
                            backupManager.restartApp()
                            "恢复完成：${s.items} 件物品、${s.zones} 个区域"
                        }.getOrElse { "恢复失败：${it.message}" }
                    }
                }) { Text("覆盖并恢复", color = Primary) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("取消") } }
        )
    }
}
