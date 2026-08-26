package com.shouna.manager.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.shouna.manager.data.db.entity.ItemPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemPhotoDao {
    @Query("SELECT * FROM item_photo WHERE itemId = :itemId ORDER BY sortOrder ASC, id ASC")
    fun observeByItem(itemId: Long): Flow<List<ItemPhotoEntity>>

    @Query("SELECT * FROM item_photo WHERE itemId = :itemId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByItem(itemId: Long): List<ItemPhotoEntity>

    @Insert
    suspend fun insert(photo: ItemPhotoEntity): Long

    @Delete
    suspend fun delete(photo: ItemPhotoEntity)

    @Query("DELETE FROM item_photo WHERE itemId = :itemId")
    suspend fun deleteByItem(itemId: Long)
}
