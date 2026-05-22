package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY timestamp DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    @Query("DELETE FROM bills")
    suspend fun clearAllBills()
}
