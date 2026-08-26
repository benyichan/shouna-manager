package com.shouna.manager.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val keyZonesSeeded = booleanPreferencesKey("zones_seeded")
    private val keyRemindHour = intPreferencesKey("remind_hour")
    private val keyRemindMinute = intPreferencesKey("remind_minute")
    private val keyBackupDirUri = stringPreferencesKey("backup_dir_uri")
    private val keyLastBackupAt = longPreferencesKey("last_backup_at")

    val zonesSeeded: Flow<Boolean> = context.dataStore.data.map { it[keyZonesSeeded] ?: false }

    suspend fun getZonesSeeded(): Boolean = zonesSeeded.first()

    suspend fun setZonesSeeded() {
        context.dataStore.edit { it[keyZonesSeeded] = true }
    }

    val remindHour: Flow<Int> = context.dataStore.data.map { it[keyRemindHour] ?: 9 }

    val remindMinute: Flow<Int> = context.dataStore.data.map { it[keyRemindMinute] ?: 0 }

    suspend fun getRemindTime(): Pair<Int, Int> {
        val data = context.dataStore.data.first()
        return (data[keyRemindHour] ?: 9) to (data[keyRemindMinute] ?: 0)
    }

    suspend fun setRemindTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[keyRemindHour] = hour
            it[keyRemindMinute] = minute
        }
    }

    val backupDirUri: Flow<String?> = context.dataStore.data.map { it[keyBackupDirUri] }

    suspend fun getBackupDirUri(): String? = backupDirUri.first()

    suspend fun setBackupDirUri(uri: String?) {
        context.dataStore.edit { if (uri == null) it.remove(keyBackupDirUri) else it[keyBackupDirUri] = uri }
    }

    val lastBackupAt: Flow<Long> = context.dataStore.data.map { it[keyLastBackupAt] ?: 0L }

    suspend fun getLastBackupAt(): Long = lastBackupAt.first()

    suspend fun setLastBackupAt(ts: Long) {
        context.dataStore.edit { it[keyLastBackupAt] = ts }
    }
}
