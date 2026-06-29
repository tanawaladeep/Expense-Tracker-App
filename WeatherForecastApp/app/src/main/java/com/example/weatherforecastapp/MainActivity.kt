package com.example.weatherforecastapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherforecastapp.data.model.WeatherData
import com.example.weatherforecastapp.databinding.ActivityMainBinding
import com.example.weatherforecastapp.ui.adapter.DailyAdapter
import com.example.weatherforecastapp.ui.adapter.HourlyAdapter
import com.example.weatherforecastapp.ui.viewmodel.WeatherUiState
import com.example.weatherforecastapp.ui.viewmodel.WeatherViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: WeatherViewModel
    
    private val hourlyAdapter = HourlyAdapter()
    private val dailyAdapter = DailyAdapter()

    // Location request permission launcher
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
        val coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (fineLocationGranted || coarseLocationGranted) {
            fetchCurrentLocationWeather()
        } else {
            Toast.makeText(this, "Location permission denied. Searching default city...", Toast.LENGTH_SHORT).show()
            viewModel.loadLastSavedCityWeather()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle edge-to-edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.swipeRefreshLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        binding.rvHourlyForecast.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = hourlyAdapter
        }
        binding.rvDailyForecast.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = dailyAdapter
            isNestedScrollingEnabled = false // Let NestedScrollView handle scrolling
        }
    }

    private fun setupListeners() {
        // Search button click
        binding.btnSearch.setOnClickListener {
            performSearch()
        }

        // Search on Enter key in keyboard
        binding.etCitySearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        // Location click
        binding.btnMyLocation.setOnClickListener {
            checkLocationPermissionsAndFetch()
        }

        // Retry button click
        binding.btnRetry.setOnClickListener {
            viewModel.loadLastSavedCityWeather()
        }

        // Swipe refresh gesture
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadLastSavedCityWeather()
        }
    }

    private fun performSearch() {
        val query = binding.etCitySearch.text.toString()
        if (query.isNotBlank()) {
            hideKeyboard()
            viewModel.fetchWeather(query)
        } else {
            Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.swipeRefreshLayout.isRefreshing = false
                when (state) {
                    is WeatherUiState.Loading -> {
                        binding.loadingContainer.visibility = View.VISIBLE
                        binding.errorContainer.visibility = View.GONE
                        binding.contentContainer.visibility = View.GONE
                    }
                    is WeatherUiState.Success -> {
                        binding.loadingContainer.visibility = View.GONE
                        binding.errorContainer.visibility = View.GONE
                        binding.contentContainer.fadeIn()
                        bindWeatherData(state.data)
                    }
                    is WeatherUiState.Error -> {
                        binding.loadingContainer.visibility = View.GONE
                        binding.errorContainer.visibility = View.VISIBLE
                        binding.contentContainer.visibility = View.GONE
                        binding.tvErrorMessage.text = state.message
                    }
                }
            }
        }
    }

    private fun bindWeatherData(data: WeatherData) {
        binding.tvCityName.text = "${data.cityName}${if (data.country != null) ", ${data.country}" else ""}"
        
        // Format current timestamp
        val currentTime = SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(Date())
        binding.tvDateTime.text = currentTime
        
        binding.tvTemperature.text = "${data.current.temperature.toInt()}°C"
        binding.tvWeatherCondition.text = getWeatherConditionDescription(data.current.weatherCode)
        binding.ivWeatherIcon.setImageResource(getWeatherIconResource(data.current.weatherCode))
        
        binding.tvHumidityValue.text = "${data.current.humidity}%"
        binding.tvWindSpeedValue.text = "${data.current.windSpeed} km/h"
        binding.tvPrecipitationValue.text = "${data.current.precipitation} mm"

        // Update adapters
        hourlyAdapter.submitList(data.hourlyItems)
        dailyAdapter.submitList(data.dailyItems)

        // Accessibility content descriptions
        binding.heroSection.contentDescription = "Current weather in ${data.cityName} is ${binding.tvWeatherCondition.text} with temperature ${data.current.temperature.toInt()} degrees Celsius."

        // dynamic background color shift
        updateDynamicBackground(data.current.weatherCode)
    }

    private fun updateDynamicBackground(code: Int) {
        val colors = when (code) {
            0, 1 -> intArrayOf(0xFF1F85DE.toInt(), 0xFF4EA4E8.toInt()) // Sunny bright blue
            2, 3 -> intArrayOf(0xFF3E5062.toInt(), 0xFF5D768E.toInt()) // Cloudy soft slate
            45, 48 -> intArrayOf(0xFF505F6A.toInt(), 0xFF768997.toInt()) // Foggy gray
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> intArrayOf(0xFF2B3A42.toInt(), 0xFF3F5D6F.toInt()) // Rainy dark blue
            95, 96, 99 -> intArrayOf(0xFF1A1C24.toInt(), 0xFF303A52.toInt()) // Thunderstorm dark indigo
            else -> intArrayOf(0xFF0F2027.toInt(), 0xFF203A43.toInt(), 0xFF2C5364.toInt()) // Default teal-black
        }
        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
        binding.mainContainer.background = gradientDrawable
    }

    private fun checkLocationPermissionsAndFetch() {
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFineLocation || hasCoarseLocation) {
            fetchCurrentLocationWeather()
        } else {
            locationPermissionRequest.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationWeather() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val provider = when {
            hasNetwork -> LocationManager.NETWORK_PROVIDER
            hasGps -> LocationManager.GPS_PROVIDER
            else -> null
        }

        if (provider == null) {
            Toast.makeText(this, "Location services are disabled in device settings.", Toast.LENGTH_LONG).show()
            return
        }

        // Display loading state instantly
        binding.loadingContainer.visibility = View.VISIBLE
        binding.errorContainer.visibility = View.GONE
        binding.contentContainer.visibility = View.GONE

        val lastKnown = locationManager.getLastKnownLocation(provider)
        if (lastKnown != null) {
            resolveCoordinatesAndLoad(lastKnown)
        } else {
            // Request single location update
            locationManager.requestSingleUpdate(provider, object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    resolveCoordinatesAndLoad(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }, null)
        }
    }

    private fun resolveCoordinatesAndLoad(location: Location) {
        val resolvedCityName = try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            // Use subAdminArea or locality or fallback
            val address = addresses?.firstOrNull()
            address?.locality ?: address?.subAdminArea ?: address?.adminArea
        } catch (e: Exception) {
            null
        }

        if (resolvedCityName != null) {
            viewModel.fetchWeather(resolvedCityName)
        } else {
            // Fallback: fetch default city
            Toast.makeText(this, "Could not resolve GPS location to city name. Using default.", Toast.LENGTH_SHORT).show()
            viewModel.loadLastSavedCityWeather()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun View.fadeIn(duration: Long = 400) {
        this.alpha = 0f
        this.visibility = View.VISIBLE
        this.animate()
            .alpha(1f)
            .setDuration(duration)
            .setListener(null)
    }

    private fun getWeatherConditionDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow Fall"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with Hail"
            else -> "Cloudy"
        }
    }

    private fun getWeatherIconResource(code: Int): Int {
        return when (code) {
            0, 1 -> R.drawable.ic_weather_clear
            2, 3 -> R.drawable.ic_weather_cloudy
            45, 48 -> R.drawable.ic_weather_fog
            51, 53, 55, 61, 63, 65, 80, 81, 82 -> R.drawable.ic_weather_rain
            71, 73, 75, 77, 85, 86 -> R.drawable.ic_weather_snow
            95, 96, 99 -> R.drawable.ic_weather_thunderstorm
            else -> R.drawable.ic_weather_cloudy
        }
    }
}