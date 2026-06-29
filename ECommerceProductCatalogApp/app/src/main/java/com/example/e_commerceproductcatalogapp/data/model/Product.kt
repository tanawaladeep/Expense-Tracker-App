package com.example.e_commerceproductcatalogapp.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Product(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("price") val price: Double,
    @SerializedName("discountPercentage") val discountPercentage: Double,
    @SerializedName("rating") val rating: Double,
    @SerializedName("stock") val stock: Int,
    @SerializedName("tags") val tags: List<String>?,
    @SerializedName("brand") val brand: String?,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("images") val images: List<String>?,
    @SerializedName("reviews") val reviews: List<Review>?
) : Serializable {
    val discountedPrice: Double
        get() = price * (1.0 - (discountPercentage / 100.0))
}
