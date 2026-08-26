package com.shouna.manager.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shouna.manager.data.db.dao.ItemDao
import com.shouna.manager.data.db.dao.ItemPhotoDao
import com.shouna.manager.data.db.dao.ItemZoneDao
import com.shouna.manager.data.db.dao.ZoneDao
import com.shouna.manager.data.db.entity.ItemEntity
import com.shouna.manager.data.db.entity.ItemPhotoEntity
import com.shouna.manager.data.db.entity.ItemZoneEntity
import com.shouna.manager.data.db.entity.SubZoneEntity
import com.shouna.manager.data.db.entity.ZoneEntity

@Database(
    entities = [
        ZoneEntity::class,
        SubZoneEntity::class,
        ItemEntity::class,
        ItemZoneEntity::class,
        ItemPhotoEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao
    abstract fun itemDao(): ItemDao
    abstract fun itemZoneDao(): ItemZoneDao
    abstract fun itemPhotoDao(): ItemPhotoDao

    companion object {
        const val NAME = "storage-manager.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NAME
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }

        /** v1 → v2：物品增加多标签字段（逗号分隔） */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `item` ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v2 → v3：消耗品补货（isConsumable/restockThreshold）+ 每件提醒提前量（remindDaysBefore） */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `item` ADD COLUMN isConsumable INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `item` ADD COLUMN restockThreshold INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `item` ADD COLUMN remindDaysBefore INTEGER")
            }
        }

        /** 备份恢复专用：关闭并清空单例，供恢复后重启进程使用 */
        fun closeForRestore() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
