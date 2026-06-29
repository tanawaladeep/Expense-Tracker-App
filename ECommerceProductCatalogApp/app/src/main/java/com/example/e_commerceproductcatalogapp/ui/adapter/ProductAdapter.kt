package com.example.e_commerceproductcatalogapp.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.e_commerceproductcatalogapp.data.model.Product
import com.example.e_commerceproductcatalogapp.databinding.ItemProductBinding

class ProductAdapter(private val onProductClick: (Product) -> Unit) :
    ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding, onProductClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val onProductClick: (Product) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.productTitle.text = product.title
            binding.productCategory.text = product.category
            binding.productRating.text = product.rating.toString()

            binding.productPrice.text = "$%.2f".format(product.discountedPrice)

            if (product.discountPercentage > 0) {
                binding.productOriginalPrice.visibility = View.VISIBLE
                binding.productOriginalPrice.text = "$%.2f".format(product.price)
                binding.productOriginalPrice.paintFlags =
                    binding.productOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                binding.discountBadge.visibility = View.VISIBLE
                binding.discountBadge.text = "-%.0f%%".format(product.discountPercentage)
            } else {
                binding.productOriginalPrice.visibility = View.GONE
                binding.discountBadge.visibility = View.GONE
            }

            Glide.with(itemView.context)
                .load(product.thumbnail)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.stat_notify_error)
                .into(binding.productImage)

            itemView.setOnClickListener {
                onProductClick(product)
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
