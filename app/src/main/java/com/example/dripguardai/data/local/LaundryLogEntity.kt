package com.example.dripguardai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "laundry_logs")
data class LaundryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cityName: String,
    val fabricsCsv: String,
    val hangingLocation: String,
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val rainProbabilityPercent: Int,
    val rainRisk: String,
    val dryingDurationHours: Double,
    val optimalDryingWindow: String,
    val summaryText: String,
    val actionableTipsCsv: String,
    val status: String = "Completed"
)
