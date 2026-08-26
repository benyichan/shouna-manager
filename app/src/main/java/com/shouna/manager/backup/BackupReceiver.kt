package com.shouna.manager.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.shouna.manager.data.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BackupScheduler.ACTION_WEEKLY) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dirUriStr = AppGraph.settingsRepository.getBackupDirUri()
                if (dirUriStr != null) {
                    val appContext = context.applicationContext
                    val manager = BackupManager(appContext)
                    manager.export(Uri.parse(dirUriStr))
                    AppGraph.settingsRepository.setLastBackupAt(System.currentTimeMillis())
                    manager.cleanupOld(Uri.parse(dirUriStr), 7)
                }
            } catch (t: Throwable) {
                android.util.Log.e("Backup", "weekly backup failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
