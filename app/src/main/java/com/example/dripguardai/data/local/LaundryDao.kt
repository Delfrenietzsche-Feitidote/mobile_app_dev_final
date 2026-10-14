package com.example.dripguardai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LaundryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LaundryLogEntity): Long

    @Query("SELECT * FROM laundry_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LaundryLogEntity>>

    @Query("DELETE FROM laundry_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM laundry_logs")
    suspend fun clearAllLogs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Query("SELECT * FROM saved_locations ORDER BY id DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Long)
}
