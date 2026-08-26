package com.shouna.manager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 主区域（如：客厅、厨房），全屋划分的顶层存放位置 */
@Entity(tableName = "zone")
data class ZoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long = 0
)
