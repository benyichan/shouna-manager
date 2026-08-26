package com.shouna.manager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 物品照片（一张物品可多张），存私有目录相对路径 photos/xxx.jpg */
@Entity(tableName = "item_photo")
data class ItemPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val path: String,
    val sortOrder: Int = 0
)
