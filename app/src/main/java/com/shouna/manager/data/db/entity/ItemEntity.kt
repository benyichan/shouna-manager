package com.shouna.manager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** 物品 */
@Entity(tableName = "item")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    /** 多标签，逗号分隔，如「食品,电子产品」 */
    val tags: String = "",
    val purchaseDate: Long? = null,
    val platform: String? = null,
    val amount: Double? = null,
    val quantity: Int = 1,
    val productionDate: Long? = null,
    /** 保质期描述，如「15天 / 9个月 / 3年」，用于自动计算有效日期 */
    val shelfLifeText: String? = null,
    /** 有效日期：直填的值，或由生产日期+保质期自动算出；null 表示无有效期 */
    val expiryDate: Long? = null,
    /** 是否直填有效日期（true=不参与自动重算） */
    val expiryDirect: Boolean = false,
    val specModel: String? = null,
    val note: String = "",
    val handled: Boolean = false,
    val handledType: String? = null,
    val handledAt: Long? = null,
    /** 消耗品：数量低于阈值时进入补货清单 */
    val isConsumable: Boolean = false,
    /** 补货阈值：quantity <= 阈值 视为需要补货 */
    val restockThreshold: Int = 1,
    /** 到期提醒提前天数；null 表示用全局默认（7 天） */
    val remindDaysBefore: Int? = null,
    val createdAt: Long,
    val updatedAt: Long = 0
)
