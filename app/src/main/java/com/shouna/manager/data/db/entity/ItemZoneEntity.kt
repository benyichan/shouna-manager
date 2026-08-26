package com.shouna.manager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 物品的存放位置关联（一件物品最多 3 个位置） */
@Entity(tableName = "item_zone")
data class ItemZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val zoneId: Long,
    /** null 表示只定位到主区域级 */
    val subZoneId: Long? = null,
    val sortOrder: Int = 0
)
