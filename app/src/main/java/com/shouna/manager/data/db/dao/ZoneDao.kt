package com.shouna.manager.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.shouna.manager.data.db.entity.SubZoneEntity
import com.shouna.manager.data.db.entity.ZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {
    @Query("SELECT * FROM zone ORDER BY sortOrder ASC, id ASC")
    fun observeZones(): Flow<List<ZoneEntity>>

    @Query("SELECT * FROM zone ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllZones(): List<ZoneEntity>

    @Query("SELECT * FROM zone WHERE id = :id")
    suspend fun getZone(id: Long): ZoneEntity?

    @Insert
    suspend fun insertZone(zone: ZoneEntity): Long

    @Update
    suspend fun updateZone(zone: ZoneEntity)

    @Delete
    suspend fun deleteZone(zone: ZoneEntity)

    @Query("SELECT * FROM sub_zone WHERE zoneId = :zoneId ORDER BY id ASC")
    fun observeSubZones(zoneId: Long): Flow<List<SubZoneEntity>>

    @Query("SELECT * FROM sub_zone ORDER BY id ASC")
    fun observeAllSubZones(): Flow<List<SubZoneEntity>>

    @Query("SELECT * FROM sub_zone ORDER BY id ASC")
    suspend fun getAllSubZones(): List<SubZoneEntity>

    @Query("SELECT * FROM sub_zone WHERE id = :id")
    suspend fun getSubZone(id: Long): SubZoneEntity?

    @Insert
    suspend fun insertSubZone(sub: SubZoneEntity): Long

    @Update
    suspend fun updateSubZone(sub: SubZoneEntity)

    @Delete
    suspend fun deleteSubZone(sub: SubZoneEntity)
}
