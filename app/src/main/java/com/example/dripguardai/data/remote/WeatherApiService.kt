package com.example.dripguardai.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

// Current Weather API Response (/data/2.5/weather)
data class CurrentWeatherResponse(
    @SerializedName("coord") val coord: CoordInfo?,
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("main") val main: MainWeather,
    @SerializedName("wind") val wind: WindInfo,
    @SerializedName("rain") val rain: RainVolume?,
    @SerializedName("name") val name: String,
    @SerializedName("sys") val sys: SysInfo?
)

data class CoordInfo(
    @SerializedName("lon") val lon: Double,
    @SerializedName("lat") val lat: Double
)

data class RainVolume(
    @SerializedName("1h") val oneHour: Double?,
    @SerializedName("3h") val threeHour: Double?
)

data class SysInfo(
    @SerializedName("country") val country: String?
)

// 5-Day / 3-Hour Forecast API Response (/data/2.5/forecast)
data class ForecastResponse(
    @SerializedName("list") val list: List<ForecastItem>,
    @SerializedName("city") val city: CityInfo
)

data class ForecastItem(
    @SerializedName("dt") val dt: Long,
    @SerializedName("main") val main: MainWeather,
    @SerializedName("weather") val weather: List<WeatherDescription>,
    @SerializedName("wind") val wind: WindInfo,
    @SerializedName("pop") val pop: Double = 0.0,
    @SerializedName("dt_txt") val dtTxt: String?
)

data class MainWeather(
    @SerializedName("temp") val temp: Double,
    @SerializedName("feels_like") val feelsLike: Double,
    @SerializedName("temp_min") val tempMin: Double,
    @SerializedName("temp_max") val tempMax: Double,
    @SerializedName("pressure") val pressure: Int,
    @SerializedName("humidity") val humidity: Int
)

data class WeatherDescription(
    @SerializedName("id") val id: Int,
    @SerializedName("main") val main: String,
    @SerializedName("description") val description: String,
    @SerializedName("icon") val icon: String
)

data class WindInfo(
    @SerializedName("speed") val speed: Double,
    @SerializedName("deg") val deg: Int,
    @SerializedName("gust") val gust: Double?
)

data class CityInfo(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("country") val country: String
)

interface WeatherApiService {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): CurrentWeatherResponse

    @GET("data/2.5/forecast")
    suspend fun getWeatherForecast(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): ForecastResponse
}
