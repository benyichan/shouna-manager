package com.shouna.manager.data.repository

import com.shouna.manager.data.AppGraph
import com.shouna.manager.data.db.dao.ZoneDao
import com.shouna.manager.data.db.entity.SubZoneEntity
import com.shouna.manager.data.db.entity.ZoneEntity
import kotlinx.coroutines.flow.Flow

class ZoneRepository(private val dao: ZoneDao) {

    fun observeZones(): Flow<List<ZoneEntity>> = dao.observeZones()

    fun observeSubZones(zoneId: Long): Flow<List<SubZoneEntity>> = dao.observeSubZones(zoneId)

    fun observeAllSubZones(): Flow<List<SubZoneEntity>> = dao.observeAllSubZones()

    suspend fun getAllZones(): List<ZoneEntity> = dao.getAllZones()

    suspend fun getAllSubZones(): List<SubZoneEntity> = dao.getAllSubZones()

    suspend fun getZone(id: Long): ZoneEntity? = dao.getZone(id)

    suspend fun getSubZone(id: Long): SubZoneEntity? = dao.getSubZone(id)

    suspend fun addZone(code: String, name: String, sortOrder: Int = 0): Long {
        val now = System.currentTimeMillis()
        return dao.insertZone(ZoneEntity(code = code, name = name, sortOrder = sortOrder, createdAt = now, updatedAt = now))
    }

    suspend fun updateZone(zone: ZoneEntity) =
        dao.updateZone(zone.copy(updatedAt = System.currentTimeMillis()))

    /** 删除主区域：连带删除其子区域及这些位置上的物品关联（物品本身保留） */
    suspend fun deleteZone(zone: ZoneEntity) {
        getAllSubZones().filter { it.zoneId == zone.id }.forEach { dao.deleteSubZone(it) }
        AppGraph.database.itemZoneDao().deleteByZone(zone.id)
        dao.deleteZone(zone)
    }

    suspend fun addSubZone(zoneId: Long, name: String): Long {
        val now = System.currentTimeMillis()
        return dao.insertSubZone(SubZoneEntity(zoneId = zoneId, name = name, createdAt = now, updatedAt = now))
    }

    suspend fun updateSubZone(sub: SubZoneEntity) =
        dao.updateSubZone(sub.copy(updatedAt = System.currentTimeMillis()))

    /** 删除子区域：清理该子区域上的物品位置关联（物品本身保留） */
    suspend fun deleteSubZone(sub: SubZoneEntity) {
        AppGraph.database.itemZoneDao().deleteBySubZone(sub.id)
        dao.deleteSubZone(sub)
    }
}
