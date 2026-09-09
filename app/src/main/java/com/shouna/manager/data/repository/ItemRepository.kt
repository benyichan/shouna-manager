package com.shouna.manager.data.repository

import android.content.Context
import com.shouna.manager.data.PhotoStorage
import com.shouna.manager.data.db.dao.ItemDao
import com.shouna.manager.data.db.dao.ItemPhotoDao
import com.shouna.manager.data.db.dao.ItemZoneDao
import com.shouna.manager.data.db.entity.ItemEntity
import com.shouna.manager.data.db.entity.ItemPhotoEntity
import com.shouna.manager.data.db.entity.ItemZoneEntity
import com.shouna.manager.widget.WidgetSync
import kotlinx.coroutines.flow.Flow

class ItemRepository(
    private val itemDao: ItemDao,
    private val itemZoneDao: ItemZoneDao,
    private val itemPhotoDao: ItemPhotoDao,
    private val context: Context
) {
    fun observeAllItems(): Flow<List<ItemEntity>> = itemDao.observeAllItems()

    suspend fun getAllItems(): List<ItemEntity> = itemDao.getAllItems()

    suspend fun getItem(id: Long): ItemEntity? = itemDao.getItem(id)

    fun observeItem(id: Long): Flow<ItemEntity?> = itemDao.observeItem(id)

    fun observeItemZones(itemId: Long): Flow<List<ItemZoneEntity>> = itemZoneDao.observeByItem(itemId)

    suspend fun getItemZones(itemId: Long): List<ItemZoneEntity> = itemZoneDao.getByItem(itemId)

    suspend fun getAllItemZones(): List<ItemZoneEntity> = itemZoneDao.getAll()

    fun observeAllItemZones(): Flow<List<ItemZoneEntity>> = itemZoneDao.observeAll()

    fun observePhotos(itemId: Long): Flow<List<ItemPhotoEntity>> = itemPhotoDao.observeByItem(itemId)

    suspend fun getPhotos(itemId: Long): List<ItemPhotoEntity> = itemPhotoDao.getByItem(itemId)

    suspend fun getExpiring(from: Long, to: Long): List<ItemEntity> = itemDao.getExpiring(from, to)

    suspend fun getExpired(now: Long): List<ItemEntity> = itemDao.getExpired(now)

    suspend fun getNeedRemind(to: Long): List<ItemEntity> = itemDao.getNeedRemind(to)

    /** 需要补货的消耗品（数量 ≤ 阈值） */
    suspend fun getNeedRestock(): List<ItemEntity> = itemDao.getNeedRestock()

    /** 新增物品：插入 item 并写入位置与照片关联 */
    suspend fun add(
        item: ItemEntity,
        zones: List<ItemZoneEntity>,
        photoPaths: List<String>
    ): Long {
        val id = itemDao.insertItem(item)
        replaceRelations(id, zones, photoPaths)
        WidgetSync.refresh(context)
        return id
    }

    /** 更新物品：更新 item，重写位置与照片关联（照片文件不在此删除） */
    suspend fun update(
        item: ItemEntity,
        zones: List<ItemZoneEntity>,
        photoPaths: List<String>
    ) {
        itemDao.updateItem(item.copy(updatedAt = System.currentTimeMillis()))
        replaceRelations(item.id, zones, photoPaths)
        WidgetSync.refresh(context)
    }

    /** 标记已处理（已食用/已丢弃/已转移），停止到期提醒 */
    suspend fun markHandled(item: ItemEntity, type: String) {
        val now = System.currentTimeMillis()
        itemDao.updateItem(
            item.copy(handled = true, handledType = type, handledAt = now, updatedAt = now)
        )
        WidgetSync.refresh(context)
    }

    /** 撤销处理 */
    suspend fun unhandle(item: ItemEntity) {
        itemDao.updateItem(
            item.copy(handled = false, handledType = null, handledAt = null, updatedAt = System.currentTimeMillis())
        )
        WidgetSync.refresh(context)
    }

    /** 删除物品：删照片文件、照片记录、位置关联、物品 */
    suspend fun delete(item: ItemEntity) {
        getPhotos(item.id).forEach { PhotoStorage.delete(context, it.path) }
        itemPhotoDao.deleteByItem(item.id)
        itemZoneDao.deleteByItem(item.id)
        itemDao.deleteItem(item)
        WidgetSync.refresh(context)
    }

    /** 单独删除一张照片（删文件 + 删记录） */
    suspend fun deletePhoto(photo: ItemPhotoEntity) {
        PhotoStorage.delete(context, photo.path)
        itemPhotoDao.delete(photo)
    }

    private suspend fun replaceRelations(itemId: Long, zones: List<ItemZoneEntity>, photoPaths: List<String>) {
        itemZoneDao.deleteByItem(itemId)
        zones.take(3).forEachIndexed { i, z ->
            itemZoneDao.insert(z.copy(itemId = itemId, sortOrder = i))
        }
        itemPhotoDao.deleteByItem(itemId)
        photoPaths.forEachIndexed { i, p ->
            itemPhotoDao.insert(ItemPhotoEntity(itemId = itemId, path = p, sortOrder = i))
        }
    }
}
