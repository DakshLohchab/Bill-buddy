package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val smsSender: String,
    val smsBody: String,
    val amount: Double,
    val merchant: String,
    val timestamp: Long = System.currentTimeMillis()
)
