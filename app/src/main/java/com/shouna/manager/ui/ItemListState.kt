package com.shouna.manager.ui

import com.shouna.manager.data.db.entity.ItemZoneEntity

/** 把一个物品的位置关联拼成可读标签 */
fun locationLabels(
    itemId: Long,
    zonesByItem: Map<Long, List<ItemZoneEntity>>,
    zoneNameById: Map<Long, String>,
    subZoneNameById: Map<Long, String>
): List<String> =
    zonesByItem[itemId].orEmpty().map { iz ->
        val z = zoneNameById[iz.zoneId].orEmpty()
        val s = iz.subZoneId?.let { subZoneNameById[it] }
        listOfNotNull(z, s).joinToString("·")
    }

fun locationLabel(
    itemId: Long,
    zonesByItem: Map<Long, List<ItemZoneEntity>>,
    zoneNameById: Map<Long, String>,
    subZoneNameById: Map<Long, String>
): String = locationLabels(itemId, zonesByItem, zoneNameById, subZoneNameById).joinToString(" / ")
