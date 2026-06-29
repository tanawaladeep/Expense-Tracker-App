package com.example.weatherforecastapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherforecastapp.R
import com.example.weatherforecastapp.data.model.DailyItem
import com.example.weatherforecastapp.databinding.ItemDailyBinding

class DailyAdapter : RecyclerView.Adapter<DailyAdapter.DailyViewHolder>() {

    private val items = mutableListOf<DailyItem>()

    fun submitList(newItems: List<DailyItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyViewHolder {
        val binding = ItemDailyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DailyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DailyViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class DailyViewHolder(private val binding: ItemDailyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DailyItem) {
            binding.tvDailyDay.text = item.dateFormatted
            binding.tvDailyMaxTemp.text = "${item.tempMax.toInt()}°"
            binding.tvDailyMinTemp.text = "${item.tempMin.toInt()}°"
            binding.ivDailyIcon.setImageResource(getWeatherIconResource(item.weatherCode))
            
            // Set accessibility text
            binding.root.contentDescription = "${item.dateFormatted}, high of ${item.tempMax.toInt()} degrees, low of ${item.tempMin.toInt()} degrees"
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
