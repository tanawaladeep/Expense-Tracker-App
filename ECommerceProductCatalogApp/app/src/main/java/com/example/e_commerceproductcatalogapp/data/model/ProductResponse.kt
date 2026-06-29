package com.example.e_commerceproductcatalogapp.data.model

import com.google.gson.annotations.SerializedName

data class ProductResponse(
    @SerializedName("products") val products: List<Product>,
    @SerializedName("total") val total: Int,
    @SerializedName("skip") val skip: Int,
    @SerializedName("limit") val limit: Int
)
