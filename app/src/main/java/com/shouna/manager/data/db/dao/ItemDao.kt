package com.shouna.manager.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shouna.manager.data.db.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("SELECT * FROM item ORDER BY createdAt DESC, id DESC")
    fun observeAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM item ORDER BY createdAt DESC, id DESC")
    suspend fun getAllItems(): List<ItemEntity>

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun getItem(id: Long): ItemEntity?

    @Query("SELECT * FROM item WHERE id = :id")
    fun observeItem(id: Long): Flow<ItemEntity?>

    /** 临期（未处理，有效日期在 [from, to]） */
    @Query("SELECT * FROM item WHERE handled = 0 AND expiryDate IS NOT NULL AND expiryDate >= :from AND expiryDate <= :to ORDER BY expiryDate ASC")
    suspend fun getExpiring(from: Long, to: Long): List<ItemEntity>

    /** 已过期（未处理，有效日期早于 now） */
    @Query("SELECT * FROM item WHERE handled = 0 AND expiryDate IS NOT NULL AND expiryDate < :now ORDER BY expiryDate ASC")
    suspend fun getExpired(now: Long): List<ItemEntity>

    /** 需要提醒：未处理且有效日期已到/临近（≤ to），供每日闹钟查询 */
    @Query("SELECT * FROM item WHERE handled = 0 AND expiryDate IS NOT NULL AND expiryDate <= :to ORDER BY expiryDate ASC")
    suspend fun getNeedRemind(to: Long): List<ItemEntity>

    /** 需要补货：消耗品且数量 ≤ 阈值 */
    @Query("SELECT * FROM item WHERE isConsumable = 1 AND quantity <= restockThreshold ORDER BY name ASC")
    suspend fun getNeedRestock(): List<ItemEntity>
}
