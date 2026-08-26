package com.shouna.manager.data

import android.content.Context
import com.shouna.manager.data.db.AppDatabase
import com.shouna.manager.data.repository.ItemRepository
import com.shouna.manager.data.repository.ZoneRepository
import com.shouna.manager.data.settings.SettingsRepository

object AppGraph {
    lateinit var context: Context
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var zoneRepository: ZoneRepository
        private set
    lateinit var itemRepository: ItemRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    fun init(context: Context) {
        if (::database.isInitialized) return
        this.context = context.applicationContext
        database = AppDatabase.get(this.context)
        zoneRepository = ZoneRepository(database.zoneDao())
        itemRepository = ItemRepository(
            database.itemDao(),
            database.itemZoneDao(),
            database.itemPhotoDao(),
            this.context
        )
        settingsRepository = SettingsRepository(this.context)
    }
}
