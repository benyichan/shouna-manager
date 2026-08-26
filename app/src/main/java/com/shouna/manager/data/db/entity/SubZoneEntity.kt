package com.shouna.manager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 子区域：挂在某个主区域下，如「厨房 → 冰箱冷冻层」 */
@Entity(tableName = "sub_zone")
data class SubZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zoneId: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long = 0
)
