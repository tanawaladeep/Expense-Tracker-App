package com.example.weatherforecastapp.data.repository

import com.example.weatherforecastapp.data.api.WeatherApi
import com.example.weatherforecastapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class WeatherRepository(private val api: WeatherApi) {

    suspend fun getWeatherData(cityName: String): WeatherData = withContext(Dispatchers.IO) {
        // 1. Search for the city coordinates
        val geocodingResponse = try {
            api.searchCity(cityName)
        } catch (e: Exception) {
            throw Exception("Failed to connect to the geocoding service. Please check your connection.", e)
        }

        val results = geocodingResponse.results
        if (results.isNullOrEmpty()) {
            throw Exception("City '$cityName' not found. Please try another name.")
        }

        // Take the best match (first result)
        val location = results[0]

        // 2. Fetch the forecast for these coordinates
        val forecastResponse = try {
            api.getForecast(location.latitude, location.longitude)
        } catch (e: Exception) {
            throw Exception("Failed to fetch weather forecast for ${location.name}.", e)
        }

        val current = forecastResponse.current ?: throw Exception("Current weather details are unavailable.")
        val hourly = forecastResponse.hourly ?: throw Exception("Hourly forecast details are unavailable.")
        val daily = forecastResponse.daily ?: throw Exception("Daily forecast details are unavailable.")

        // 3. Map hourly items (limit to next 24 hours for dashboard performance)
        val hourlyItems = mutableListOf<HourlyItem>()
        val currentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        val size = minOf(hourly.time.size, hourly.temperatures.size, hourly.weatherCodes.size)
        // Find current time index or start from the nearest hour
        val currentHourString = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.US).format(Date())
        var startIndex = hourly.time.indexOf(currentHourString)
        if (startIndex == -1) {
            startIndex = 0
        }

        // Fetch 24 hours of data starting from current/nearest hour
        val endIndex = minOf(startIndex + 24, size)
        for (i in startIndex until endIndex) {
            val rawTime = hourly.time[i]
            val temp = hourly.temperatures[i]
            val code = hourly.weatherCodes[i]
            val humidityVal = hourly.humidities.getOrNull(i) ?: 0
            
            // Format time: "2026-06-29T14:00" -> "2 PM" or "14:00"
            val formattedTime = formatHourlyTime(rawTime)
            
            // Check if it's day or night for the icon
            val hourPart = rawTime.split("T").getOrNull(1)?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 12
            val isDayHour = hourPart in 6..18

            hourlyItems.add(HourlyItem(
                rawTime = rawTime,
                timeFormatted = formattedTime,
                temperature = temp,
                weatherCode = code,
                isDay = isDayHour
            ))
        }

        // 4. Map daily items
        val dailyItems = mutableListOf<DailyItem>()
        val dailySize = minOf(daily.time.size, daily.maxTemperatures.size, daily.minTemperatures.size, daily.weatherCodes.size)
        for (i in 0 until dailySize) {
            val rawDate = daily.time[i]
            val code = daily.weatherCodes[i]
            val maxTemp = daily.maxTemperatures[i]
            val minTemp = daily.minTemperatures[i]

            val formattedDate = formatDailyDate(rawDate, i == 0)

            dailyItems.add(DailyItem(
                rawDate = rawDate,
                dateFormatted = formattedDate,
                weatherCode = code,
                tempMax = maxTemp,
                tempMin = minTemp
            ))
        }

        // 5. Build and return consolidated UI model
        WeatherData(
            cityName = location.name,
            country = location.country,
            admin1 = location.admin1,
            current = current,
            hourlyItems = hourlyItems,
            dailyItems = dailyItems
        )
    }

    private fun formatHourlyTime(rawTime: String): String {
        return try {
            val timePart = rawTime.split("T").getOrNull(1) ?: return rawTime
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h a", Locale.getDefault()) // e.g., "3 PM"
            val date = inputFormat.parse(timePart)
            if (date != null) {
                // If it's the current hour, we can display "Now"
                val currentHourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                val itemHourStr = SimpleDateFormat("HH", Locale.getDefault()).format(date)
                if (currentHourStr == itemHourStr) {
                    "Now"
                } else {
                    outputFormat.format(date)
                }
            } else {
                timePart
            }
        } catch (e: Exception) {
            rawTime
        }
    }

    private fun formatDailyDate(rawDate: String, isToday: Boolean): String {
        if (isToday) return "Today"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE", Locale.getDefault()) // e.g., "Monday"
            val date = inputFormat.parse(rawDate)
            if (date != null) {
                // If it's tomorrow, we can show "Tomorrow"
                val calItem = Calendar.getInstance().apply { time = date }
                val calTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                if (calItem.get(Calendar.YEAR) == calTomorrow.get(Calendar.YEAR) &&
                    calItem.get(Calendar.DAY_OF_YEAR) == calTomorrow.get(Calendar.DAY_OF_YEAR)) {
                    "Tomorrow"
                } else {
                    outputFormat.format(date)
                }
            } else {
                rawDate
            }
        } catch (e: Exception) {
            rawDate
        }
    }
}
