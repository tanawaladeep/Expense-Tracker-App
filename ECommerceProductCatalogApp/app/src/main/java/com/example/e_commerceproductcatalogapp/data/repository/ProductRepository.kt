package com.example.e_commerceproductcatalogapp.data.repository

import com.example.e_commerceproductcatalogapp.data.api.ProductService
import com.example.e_commerceproductcatalogapp.data.model.ProductResponse

class ProductRepository(private val productService: ProductService) {
    suspend fun getProducts(limit: Int, skip: Int): ProductResponse {
        return productService.getProducts(limit, skip)
    }

    suspend fun searchProducts(query: String, limit: Int, skip: Int): ProductResponse {
        return productService.searchProducts(query, limit, skip)
    }

    suspend fun getProductsByCategory(category: String, limit: Int, skip: Int): ProductResponse {
        return productService.getProductsByCategory(category, limit, skip)
    }

    suspend fun getCategories(): List<String> {
        return productService.getCategories()
    }
}
