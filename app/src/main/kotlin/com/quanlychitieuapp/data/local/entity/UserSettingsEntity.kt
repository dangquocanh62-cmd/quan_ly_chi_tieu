package com.quanlychitieuapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User settings stored in Room.
 * Single row (id = 1) - singleton pattern.
 */
@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val currency: String = "VND",
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,         // default 21:00
    val reminderMinute: Int = 0,
    val reminderMessage: String = "Hôm nay bạn đã nhập đầy đủ chi tiêu chưa?",
    val confirmationThreshold: Long = 1_000_000L, // 1 triệu, configurable
    val geminiEnabled: Boolean = false,
    val defaultChartPeriod: String = "monthly",  // daily/weekly/monthly/yearly
    val isDarkMode: Boolean = false,
    val debtConfirmationThreshold: Long = 1_000_000L,
    val dbSchemaVersion: Int = 1
)
