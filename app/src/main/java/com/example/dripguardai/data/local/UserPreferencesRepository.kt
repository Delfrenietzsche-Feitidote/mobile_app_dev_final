package com.example.dripguardai.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val TEMP_UNIT_KEY = stringPreferencesKey("temperature_unit")
        val DEFAULT_DRYING_SETUP_KEY = stringPreferencesKey("default_drying_setup")

        const val UNIT_CELSIUS = "CELSIUS"
        const val UNIT_FAHRENHEIT = "FAHRENHEIT"

        const val SETUP_OUTDOOR = "Outdoor Direct Sun"
        const val SETUP_INDOOR = "Indoor Well-Ventilated"
    }

    val temperatureUnitFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TEMP_UNIT_KEY] ?: UNIT_CELSIUS
    }

    val defaultDryingSetupFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEFAULT_DRYING_SETUP_KEY] ?: SETUP_OUTDOOR
    }

    suspend fun setTemperatureUnit(unit: String) {
        context.dataStore.edit { preferences ->
            preferences[TEMP_UNIT_KEY] = unit
        }
    }

    suspend fun setDefaultDryingSetup(setup: String) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_DRYING_SETUP_KEY] = setup
        }
    }
}
