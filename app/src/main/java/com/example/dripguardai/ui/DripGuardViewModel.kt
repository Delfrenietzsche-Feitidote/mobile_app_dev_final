package com.example.dripguardai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dripguardai.BuildConfig
import com.example.dripguardai.data.ai.DryingRecommendation
import com.example.dripguardai.data.ai.DryingRequest
import com.example.dripguardai.data.ai.GeminiLaundryService
import com.example.dripguardai.data.local.AppDatabase
import com.example.dripguardai.data.local.LaundryDao
import com.example.dripguardai.data.local.LaundryLogEntity
import com.example.dripguardai.data.local.LocationEntity
import com.example.dripguardai.data.local.UserPreferencesRepository
import com.example.dripguardai.data.remote.RetrofitInstance
import com.example.dripguardai.data.remote.WeatherApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

data class DashboardWeatherState(
    val cityName: String = "New York",
    val tempCelsius: Double = 22.0,
    val humidity: Int = 55,
    val windSpeed: Double = 2.8,
    val rainProb: Int = 10,
    val dryingIndex: Int = 82,
    val dryingIndexLabel: String = "Excellent",
    val quickAiAlert: String = "☀️ Clear skies & light breeze. Ideal conditions to hang laundry outside today!",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class PlannerUiState {
    object Idle : PlannerUiState()
    object Loading : PlannerUiState()
    data class Success(
        val recommendation: DryingRecommendation,
        val cityName: String
    ) : PlannerUiState()
    data class Error(val message: String) : PlannerUiState()
}

data class DryingTimerState(
    val isRunning: Boolean = false,
    val remainingSeconds: Long = 0,
    val totalSeconds: Long = 0,
    val taskTitle: String = ""
)

private data class WeatherMetrics(
    val temp: Double,
    val humidity: Int,
    val windSpeed: Double,
    val rainProb: Int
)

class DripGuardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val userPrefsRepo = UserPreferencesRepository(application)
    private val weatherApiService: WeatherApiService = RetrofitInstance.api
    private val geminiLaundryService: GeminiLaundryService = GeminiLaundryService(BuildConfig.GEMINI_API_KEY)
    private val laundryDao: LaundryDao = AppDatabase.getDatabase(application).laundryDao()

    val temperatureUnit: StateFlow<String> = userPrefsRepo.temperatureUnitFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.UNIT_CELSIUS)

    val defaultDryingSetup: StateFlow<String> = userPrefsRepo.defaultDryingSetupFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferencesRepository.SETUP_OUTDOOR)

    private val _dashboardState = MutableStateFlow(DashboardWeatherState())
    val dashboardState: StateFlow<DashboardWeatherState> = _dashboardState.asStateFlow()

    private val _plannerState = MutableStateFlow<PlannerUiState>(PlannerUiState.Idle)
    val plannerState: StateFlow<PlannerUiState> = _plannerState.asStateFlow()

    private val _timerState = MutableStateFlow(DryingTimerState())
    val timerState: StateFlow<DryingTimerState> = _timerState.asStateFlow()
    private var timerJob: Job? = null

    val savedLogs: StateFlow<List<LaundryLogEntity>> = laundryDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedLocations: StateFlow<List<LocationEntity>> = laundryDao.getAllLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchDashboardWeather("New York")
    }

    private fun getCityWeatherMetrics(cityName: String): WeatherMetrics {
        val clean = cityName.lowercase().trim()
        val hash = abs(clean.fold(0) { acc, c -> acc * 31 + c.code })

        val temp = 10.0 + (hash % 240) / 10.0
        val humidity = 30 + ((hash / 7) % 61)
        val windSpeed = 1.0 + ((hash / 13) % 75) / 10.0
        val rainProb = ((hash / 17) % 86)

        return WeatherMetrics(temp, humidity, windSpeed, rainProb)
    }

    fun fetchDashboardWeather(cityName: String, setupOverride: String? = null) {
        if (cityName.isBlank()) return

        viewModelScope.launch {
            _dashboardState.value = _dashboardState.value.copy(isLoading = true, error = null)

            try {
                val fallback = getCityWeatherMetrics(cityName)
                var temp = fallback.temp
                var humidity = fallback.humidity
                var windSpeed = fallback.windSpeed
                var rainProb = fallback.rainProb

                val apiKey = BuildConfig.OPENWEATHER_API_KEY
                if (apiKey.isNotBlank() && apiKey != "your_openweather_api_key_here") {
                    try {
                        val currentResp = weatherApiService.getCurrentWeather(cityName.trim(), apiKey)
                        temp = currentResp.main.temp
                        humidity = currentResp.main.humidity
                        windSpeed = currentResp.wind.speed

                        try {
                            val forecast = weatherApiService.getWeatherForecast(cityName.trim(), apiKey)
                            if (forecast.list.isNotEmpty()) {
                                rainProb = ((forecast.list.take(4).maxOfOrNull { it.pop } ?: 0.0) * 100).toInt()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val hangingSetup = setupOverride ?: defaultDryingSetup.value

                val req = DryingRequest(
                    cityName = cityName.trim(),
                    temperature = temp,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    rainProbabilityPercent = rainProb,
                    fabrics = listOf("Cotton", "Synthetics/Polyester"),
                    hangingLocation = hangingSetup
                )

                val recommendation = geminiLaundryService.getLaundryRecommendation(req)

                _dashboardState.value = DashboardWeatherState(
                    cityName = cityName.trim(),
                    tempCelsius = temp,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    rainProb = rainProb,
                    dryingIndex = recommendation.dryingIndex,
                    dryingIndexLabel = recommendation.dryingIndexLabel,
                    quickAiAlert = recommendation.quickAiAlert,
                    isLoading = false
                )
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to load weather data."
                )
            }
        }
    }

    fun calculatePlannerDrying(
        cityName: String,
        selectedFabrics: List<String>,
        hangingLocation: String
    ) {
        if (cityName.isBlank()) {
            _plannerState.value = PlannerUiState.Error("Please enter a location name.")
            return
        }
        if (selectedFabrics.isEmpty()) {
            _plannerState.value = PlannerUiState.Error("Please select at least one fabric.")
            return
        }

        viewModelScope.launch {
            _plannerState.value = PlannerUiState.Loading

            try {
                val fallback = getCityWeatherMetrics(cityName)
                var temp = fallback.temp
                var humidity = fallback.humidity
                var windSpeed = fallback.windSpeed
                var rainProb = fallback.rainProb

                val apiKey = BuildConfig.OPENWEATHER_API_KEY
                if (apiKey.isNotBlank() && apiKey != "your_openweather_api_key_here") {
                    try {
                        val currentResp = weatherApiService.getCurrentWeather(cityName.trim(), apiKey)
                        temp = currentResp.main.temp
                        humidity = currentResp.main.humidity
                        windSpeed = currentResp.wind.speed

                        try {
                            val forecast = weatherApiService.getWeatherForecast(cityName.trim(), apiKey)
                            if (forecast.list.isNotEmpty()) {
                                rainProb = ((forecast.list.take(4).maxOfOrNull { it.pop } ?: 0.0) * 100).toInt()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val req = DryingRequest(
                    cityName = cityName.trim(),
                    temperature = temp,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    rainProbabilityPercent = rainProb,
                    fabrics = selectedFabrics,
                    hangingLocation = hangingLocation
                )

                val rec = geminiLaundryService.getLaundryRecommendation(req)

                _plannerState.value = PlannerUiState.Success(
                    recommendation = rec,
                    cityName = cityName.trim()
                )
            } catch (e: Exception) {
                _plannerState.value = PlannerUiState.Error(
                    e.localizedMessage ?: "Failed to evaluate AI drying recommendations."
                )
            }
        }
    }

    fun startDryingTimer(durationHours: Double, label: String) {
        val totalSecs = (durationHours * 3600).toLong().coerceAtLeast(60)
        timerJob?.cancel()

        _timerState.value = DryingTimerState(
            isRunning = true,
            remainingSeconds = totalSecs,
            totalSeconds = totalSecs,
            taskTitle = label
        )

        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                delay(1000L)
                val newRem = _timerState.value.remainingSeconds - 1
                if (newRem <= 0) {
                    _timerState.value = _timerState.value.copy(
                        isRunning = false,
                        remainingSeconds = 0
                    )
                } else {
                    _timerState.value = _timerState.value.copy(remainingSeconds = newRem)
                }
            }
        }
    }

    fun pauseDryingTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false)
    }

    fun resumeDryingTimer() {
        if (_timerState.value.remainingSeconds <= 0) return
        _timerState.value = _timerState.value.copy(isRunning = true)

        timerJob = viewModelScope.launch {
            while (_timerState.value.remainingSeconds > 0 && _timerState.value.isRunning) {
                delay(1000L)
                val newRem = _timerState.value.remainingSeconds - 1
                if (newRem <= 0) {
                    _timerState.value = _timerState.value.copy(
                        isRunning = false,
                        remainingSeconds = 0
                    )
                } else {
                    _timerState.value = _timerState.value.copy(remainingSeconds = newRem)
                }
            }
        }
    }

    fun resetDryingTimer() {
        timerJob?.cancel()
        _timerState.value = DryingTimerState()
    }

    fun saveWashLog(
        title: String,
        cityName: String,
        fabrics: List<String>,
        hangingLocation: String,
        rec: DryingRecommendation,
        status: String = "Wash Completed"
    ) {
        viewModelScope.launch {
            val log = LaundryLogEntity(
                title = title.ifBlank { "Wash Session in $cityName" },
                cityName = cityName,
                fabricsCsv = fabrics.joinToString(", "),
                hangingLocation = hangingLocation,
                temperature = _dashboardState.value.tempCelsius,
                humidity = _dashboardState.value.humidity,
                windSpeed = _dashboardState.value.windSpeed,
                rainProbabilityPercent = _dashboardState.value.rainProb,
                rainRisk = rec.rainRisk,
                dryingDurationHours = rec.dryingDurationHours,
                optimalDryingWindow = rec.optimalDryingWindow,
                summaryText = rec.summaryText,
                actionableTipsCsv = rec.actionableTips.joinToString(";"),
                status = status
            )
            laundryDao.insertLog(log)
        }
    }

    fun addSavedLocation(name: String, cityName: String) {
        if (name.isBlank() || cityName.isBlank()) return
        viewModelScope.launch {
            laundryDao.insertLocation(
                LocationEntity(
                    name = name.trim(),
                    cityName = cityName.trim()
                )
            )
        }
    }

    fun deleteSavedLocation(id: Long) {
        viewModelScope.launch {
            laundryDao.deleteLocationById(id)
        }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            laundryDao.deleteLogById(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            laundryDao.clearAllLogs()
        }
    }

    fun setTemperatureUnit(unit: String) {
        viewModelScope.launch {
            userPrefsRepo.setTemperatureUnit(unit)
        }
    }

    fun setDefaultDryingSetup(setup: String) {
        viewModelScope.launch {
            userPrefsRepo.setDefaultDryingSetup(setup)
            fetchDashboardWeather(_dashboardState.value.cityName, setupOverride = setup)
        }
    }
}
