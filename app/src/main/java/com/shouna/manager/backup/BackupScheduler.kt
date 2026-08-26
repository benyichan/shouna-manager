package com.shouna.manager.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/** 每周定时自动备份（默认周日凌晨 3 点） */
class BackupScheduler(private val context: Context) {

    fun scheduleWeekly() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextSunday3am()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, BackupReceiver::class.java).apply { action = ACTION_WEEKLY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (t: Throwable) {
            android.util.Log.e("Backup", "scheduleWeekly failed", t)
            runCatching { alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent) }
        }
    }

    fun cancel() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, BackupReceiver::class.java).apply { action = ACTION_WEEKLY },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    /** 距上次备份是否已超过一个周期（用于启动兜底补备） */
    fun isDue(lastBackupAt: Long, now: Long = System.currentTimeMillis()): Boolean {
        if (lastBackupAt <= 0) return true
        val week = 7L * 24 * 60 * 60 * 1000
        return now - lastBackupAt >= week
    }

    private fun nextSunday3am(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
        }
        return cal.timeInMillis
    }

    companion object {
        const val REQUEST_CODE = 2002
        const val ACTION_WEEKLY = "com.shouna.manager.ACTION_BACKUP_WEEKLY"
    }
}
