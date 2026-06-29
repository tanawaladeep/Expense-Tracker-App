package com.example.weatherforecastapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherforecastapp.R
import com.example.weatherforecastapp.data.model.HourlyItem
import com.example.weatherforecastapp.databinding.ItemHourlyBinding

class HourlyAdapter : RecyclerView.Adapter<HourlyAdapter.HourlyViewHolder>() {

    private val items = mutableListOf<HourlyItem>()

    fun submitList(newItems: List<HourlyItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val binding = ItemHourlyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HourlyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class HourlyViewHolder(private val binding: ItemHourlyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HourlyItem) {
            binding.tvHourlyTime.text = item.timeFormatted
            binding.tvHourlyTemp.text = "${item.temperature.toInt()}°"
            binding.ivHourlyIcon.setImageResource(getWeatherIconResource(item.weatherCode))
            
            // Set accessibility text
            binding.root.contentDescription = "At ${item.timeFormatted}, temperature is ${item.temperature.toInt()} degrees Celsius"
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
}
