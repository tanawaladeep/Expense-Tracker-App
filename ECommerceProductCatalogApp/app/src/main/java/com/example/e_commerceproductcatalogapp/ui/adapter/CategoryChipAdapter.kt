package com.example.e_commerceproductcatalogapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.e_commerceproductcatalogapp.databinding.ItemCategoryChipBinding

class CategoryChipAdapter(
    private val onCategorySelected: (String) -> Unit
) : ListAdapter<String, CategoryChipAdapter.CategoryViewHolder>(CategoryDiffCallback()) {

    private var selectedCategory: String = ""

    fun setSelectedCategory(category: String) {
        selectedCategory = category
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding, onCategorySelected)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category, category == selectedCategory)
    }

    class CategoryViewHolder(
        private val binding: ItemCategoryChipBinding,
        private val onCategorySelected: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: String, isSelected: Boolean) {
            binding.categoryChip.text = category.replace("-", " ").replaceFirstChar { it.uppercase() }
            binding.categoryChip.isChecked = isSelected

            binding.categoryChip.setOnClickListener {
                if (isSelected) {
                    onCategorySelected("") // Deselect
                } else {
                    onCategorySelected(category)
                }
            }
        }
    }

    class CategoryDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}
