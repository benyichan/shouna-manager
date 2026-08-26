package com.shouna.manager.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shouna.manager.backup.BackupScheduler
import com.shouna.manager.data.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val (hour, minute) = AppGraph.settingsRepository.getRemindTime()
                ReminderScheduler(context).scheduleDaily(hour, minute)
                BackupScheduler(context).scheduleWeekly()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
