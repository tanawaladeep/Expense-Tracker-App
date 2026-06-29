package com.example.e_commerceproductcatalogapp.ui.activity

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.e_commerceproductcatalogapp.R
import com.example.e_commerceproductcatalogapp.data.model.Product
import com.example.e_commerceproductcatalogapp.databinding.ActivityProductDetailBinding
import com.example.e_commerceproductcatalogapp.ui.adapter.ReviewAdapter

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding
    private val reviewAdapter = ReviewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.detailToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.detailToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        @Suppress("DEPRECATION")
        val product = intent.getSerializableExtra("EXTRA_PRODUCT") as? Product

        if (product != null) {
            populateDetails(product)
        } else {
            Toast.makeText(this, "Failed to load product details.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateDetails(product: Product) {
        binding.detailTitle.text = product.title
        binding.detailCategory.text = product.category
        binding.detailBrand.text = product.brand ?: "Generic"
        binding.detailRating.text = product.rating.toString()
        binding.detailDescription.text = product.description

        binding.detailPrice.text = "$%.2f".format(product.discountedPrice)
        if (product.discountPercentage > 0) {
            binding.detailOriginalPrice.visibility = View.VISIBLE
            binding.detailOriginalPrice.text = "was $%.2f".format(product.price)
            binding.detailOriginalPrice.paintFlags =
                binding.detailOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            binding.detailOriginalPrice.visibility = View.GONE
        }

        when {
            product.stock > 20 -> {
                binding.detailStockStatus.text = "In Stock (${product.stock})"
                binding.detailStockStatus.setTextColor(ContextCompat.getColor(this, R.color.stock_green))
            }
            product.stock in 1..20 -> {
                binding.detailStockStatus.text = "Only ${product.stock} left in stock!"
                binding.detailStockStatus.setTextColor(ContextCompat.getColor(this, R.color.stock_amber))
            }
            else -> {
                binding.detailStockStatus.text = "Out of Stock"
                binding.detailStockStatus.setTextColor(ContextCompat.getColor(this, R.color.stock_red))
                binding.btnBuyNow.isEnabled = false
                binding.btnBuyNow.text = "Unavailable"
            }
        }

        Glide.with(this)
            .load(product.thumbnail)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.stat_notify_error)
            .into(binding.detailImage)

        binding.btnBuyNow.setOnClickListener {
            Toast.makeText(this, "${product.title} added to cart!", Toast.LENGTH_SHORT).show()
        }

        binding.rvReviews.layoutManager = LinearLayoutManager(this)
        binding.rvReviews.adapter = reviewAdapter

        val reviews = product.reviews ?: emptyList()
        if (reviews.isEmpty()) {
            binding.rvReviews.visibility = View.GONE
            binding.tvNoReviews.visibility = View.VISIBLE
        } else {
            binding.rvReviews.visibility = View.VISIBLE
            binding.tvNoReviews.visibility = View.GONE
            reviewAdapter.submitList(reviews)
        }
    }
}
