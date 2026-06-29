package com.example.e_commerceproductcatalogapp

import com.example.e_commerceproductcatalogapp.data.model.Product
import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun testProductDiscountPriceCalculation() {
        val product = Product(
            id = 1,
            title = "Test Product",
            description = "Test Description",
            category = "Test Category",
            price = 100.0,
            discountPercentage = 12.5,
            rating = 4.5,
            stock = 10,
            tags = emptyList(),
            brand = "Test Brand",
            thumbnail = "",
            images = emptyList(),
            reviews = emptyList()
        )
        // 100.0 * (1 - 12.5 / 100) = 87.5
        assertEquals(87.5, product.discountedPrice, 0.01)
    }

    @Test
    fun testProductNoDiscountPriceCalculation() {
        val product = Product(
            id = 2,
            title = "Another Product",
            description = "Description",
            category = "Category",
            price = 49.99,
            discountPercentage = 0.0,
            rating = 4.0,
            stock = 5,
            tags = emptyList(),
            brand = "Brand",
            thumbnail = "",
            images = emptyList(),
            reviews = emptyList()
        )
        assertEquals(49.99, product.discountedPrice, 0.01)
    }
}