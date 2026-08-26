package com.shouna.manager

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.shouna.manager.backup.BackupManager
import com.shouna.manager.backup.BackupScheduler
import com.shouna.manager.data.AppGraph
import com.shouna.manager.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Date

class ShounaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        installCrashLogger()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { seedDefaultZones() }
            runCatching {
                val (hour, minute) = AppGraph.settingsRepository.getRemindTime()
                ReminderScheduler(this@ShounaApp).scheduleDaily(hour, minute)
            }
            // 每周备份：重排闹钟 + 启动兜底（距上次超周期则补一次）
            runCatching {
                val scheduler = BackupScheduler(this@ShounaApp)
                scheduler.scheduleWeekly()
                val dirStr = AppGraph.settingsRepository.getBackupDirUri()
                if (dirStr != null && scheduler.isDue(AppGraph.settingsRepository.getLastBackupAt())) {
                    val manager = BackupManager(this@ShounaApp)
                    manager.export(Uri.parse(dirStr))
                    AppGraph.settingsRepository.setLastBackupAt(System.currentTimeMillis())
                    manager.cleanupOld(Uri.parse(dirStr), 7)
                }
            }.onFailure {
                android.util.Log.e("Backup", "weekly backup setup failed", it)
            }
        }
    }

    /** 首次启动预置 7 个主区域 */
    private suspend fun seedDefaultZones() {
        if (AppGraph.settingsRepository.getZonesSeeded()) return
        val names = listOf("客厅", "卧室", "厨房", "卫生间", "阳台", "储物间", "书房")
        names.forEachIndexed { i, name ->
            AppGraph.zoneRepository.addZone("区域${i + 1}", name, i)
        }
        AppGraph.settingsRepository.setZonesSeeded()
    }

    /** 捕获崩溃并把堆栈写到「下载」文件夹，便于真机排查 */
    private fun installCrashLogger() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val content = "时间：${Date()}\n线程：${thread.name}\n\n$sw\n"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "shouna-crash-${System.currentTimeMillis()}.txt")
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch (_: Exception) {
                // 日志写入失败不影响崩溃流程
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
