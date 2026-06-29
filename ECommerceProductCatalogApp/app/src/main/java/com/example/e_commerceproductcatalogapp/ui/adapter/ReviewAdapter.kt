package com.example.e_commerceproductcatalogapp.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.e_commerceproductcatalogapp.data.model.Review
import com.example.e_commerceproductcatalogapp.databinding.ItemReviewBinding
import java.util.Locale

class ReviewAdapter : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.reviewerName.text = review.reviewerName
            binding.reviewRating.text = review.rating.toString()
            binding.reviewComment.text = review.comment

            // Format date string (extract date part if full timestamp)
            val dateStr = if (review.date.length >= 10) review.date.substring(0, 10) else review.date
            binding.reviewDate.text = dateStr

            // Generate Avatar Initials
            binding.reviewerAvatar.text = getInitials(review.reviewerName)
        }

        private fun getInitials(name: String): String {
            val parts = name.trim().split("\\s+".toRegex())
            return when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts[0].take(1).uppercase(Locale.getDefault())
                else -> (parts[0].take(1) + parts[parts.size - 1].take(1)).uppercase(Locale.getDefault())
            }
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem.reviewerEmail == newItem.reviewerEmail && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem == newItem
        }
    }
}
