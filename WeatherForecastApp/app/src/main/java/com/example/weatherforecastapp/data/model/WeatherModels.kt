package com.example.weatherforecastapp.data.model

import com.google.gson.annotations.SerializedName

// --- GEOCODING API MODELS ---
data class GeocodingResponse(
    @SerializedName("results") val results: List<GeocodingResult>?
)

data class GeocodingResult(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("country") val country: String?,
    @SerializedName("admin1") val admin1: String? // State/Province
)

// --- FORECAST API MODELS ---
data class ForecastResponse(
    @SerializedName("current") val current: CurrentWeather?,
    @SerializedName("hourly") val hourly: HourlyForecast?,
    @SerializedName("daily") val daily: DailyForecast?
)

data class CurrentWeather(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("relative_humidity_2m") val humidity: Int,
    @SerializedName("apparent_temperature") val apparentTemperature: Double,
    @SerializedName("is_day") val isDay: Int,
    @SerializedName("precipitation") val precipitation: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed: Double
)

data class HourlyForecast(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m") val temperatures: List<Double>,
    @SerializedName("relative_humidity_2m") val humidities: List<Int>,
    @SerializedName("weather_code") val weatherCodes: List<Int>
)

data class DailyForecast(
    @SerializedName("time") val time: List<String>,
    @SerializedName("weather_code") val weatherCodes: List<Int>,
    @SerializedName("temperature_2m_max") val maxTemperatures: List<Double>,
    @SerializedName("temperature_2m_min") val minTemperatures: List<Double>
)

// --- CLEAN UI STATE MODELS ---
data class WeatherData(
    val cityName: String,
    val country: String?,
    val admin1: String?,
    val current: CurrentWeather,
    val hourlyItems: List<HourlyItem>,
    val dailyItems: List<DailyItem>
)

data class HourlyItem(
    val rawTime: String,
    val timeFormatted: String, // e.g. "14:00"
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Boolean
)

data class DailyItem(
    val rawDate: String,
    val dateFormatted: String, // e.g. "Mon, Jun 29"
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double
)
