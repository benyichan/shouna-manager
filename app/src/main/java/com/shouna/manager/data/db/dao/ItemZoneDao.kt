package com.shouna.manager.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.shouna.manager.data.db.entity.ItemZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemZoneDao {
    @Query("SELECT * FROM item_zone WHERE itemId = :itemId ORDER BY sortOrder ASC, id ASC")
    fun observeByItem(itemId: Long): Flow<List<ItemZoneEntity>>

    @Query("SELECT * FROM item_zone WHERE itemId = :itemId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByItem(itemId: Long): List<ItemZoneEntity>

    @Query("SELECT * FROM item_zone ORDER BY id ASC")
    fun observeAll(): Flow<List<ItemZoneEntity>>

    @Query("SELECT * FROM item_zone ORDER BY id ASC")
    suspend fun getAll(): List<ItemZoneEntity>

    @Insert
    suspend fun insert(iz: ItemZoneEntity): Long

    @Delete
    suspend fun delete(iz: ItemZoneEntity)

    @Query("DELETE FROM item_zone WHERE itemId = :itemId")
    suspend fun deleteByItem(itemId: Long)

    @Query("DELETE FROM item_zone WHERE zoneId = :zoneId")
    suspend fun deleteByZone(zoneId: Long)

    @Query("DELETE FROM item_zone WHERE subZoneId = :subZoneId")
    suspend fun deleteBySubZone(subZoneId: Long)
}
