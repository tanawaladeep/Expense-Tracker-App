package com.example.weatherforecastapp.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherforecastapp.data.api.WeatherApi
import com.example.weatherforecastapp.data.model.WeatherData
import com.example.weatherforecastapp.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(val data: WeatherData) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WeatherRepository by lazy {
        WeatherRepository(WeatherApi.create())
    }

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val sharedPrefs = application.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_CITY = "last_searched_city"
        private const val DEFAULT_CITY = "London"
    }

    init {
        loadLastSavedCityWeather()
    }

    fun loadLastSavedCityWeather() {
        val lastCity = sharedPrefs.getString(KEY_LAST_CITY, DEFAULT_CITY) ?: DEFAULT_CITY
        fetchWeather(lastCity)
    }

    fun fetchWeather(cityName: String) {
        if (cityName.isBlank()) {
            _uiState.value = WeatherUiState.Error("City name cannot be empty.")
            return
        }
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val data = repository.getWeatherData(cityName.trim())
                _uiState.value = WeatherUiState.Success(data)
                // Save city only on successful retrieval
                sharedPrefs.edit().putString(KEY_LAST_CITY, data.cityName).apply()
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }
}
