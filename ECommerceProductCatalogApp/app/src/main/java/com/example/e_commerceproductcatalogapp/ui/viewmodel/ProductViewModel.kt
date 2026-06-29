package com.example.e_commerceproductcatalogapp.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.e_commerceproductcatalogapp.data.model.Product
import com.example.e_commerceproductcatalogapp.data.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Resource<out T> {
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
}

enum class SortOption {
    NONE, PRICE_ASC, PRICE_DESC, RATING_DESC
}

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _productsState = MutableLiveData<Resource<List<Product>>>()
    val productsState: LiveData<Resource<List<Product>>> = _productsState

    private val _categoriesState = MutableLiveData<Resource<List<String>>>()
    val categoriesState: LiveData<Resource<List<String>>> = _categoriesState

    // Track fetched products
    private val _rawProducts = MutableLiveData<List<Product>>(emptyList())

    // Filter and Sort states
    val selectedCategory = MutableLiveData<String>("")
    val searchQuery = MutableLiveData<String>("")
    val selectedSort = MutableLiveData<SortOption>(SortOption.NONE)
    val minPrice = MutableLiveData<Double?>(null)
    val maxPrice = MutableLiveData<Double?>(null)

    // Pagination states
    private var skip = 0
    private val limit = 20
    private var total = 0
    var isLastPage = false
        private set

    private val _isLoadingMore = MutableLiveData<Boolean>(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private var searchJob: Job? = null

    init {
        fetchCategories()
        loadProducts(isRefresh = true)
    }

    fun fetchCategories() {
        _categoriesState.value = Resource.Loading
        viewModelScope.launch {
            try {
                val list = repository.getCategories()
                _categoriesState.value = Resource.Success(list)
            } catch (e: Exception) {
                _categoriesState.value = Resource.Error(e.message ?: "Failed to fetch categories")
            }
        }
    }

    fun setCategory(category: String) {
        if (selectedCategory.value != category) {
            selectedCategory.value = category
            searchQuery.value = ""
            loadProducts(isRefresh = true)
        }
    }

    fun setSearchQuery(query: String) {
        // Instantly update query and reset category to enable 0ms local filtering
        searchQuery.value = query
        selectedCategory.value = ""
        applyLocalFilterAndSort()

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(150) // Faster debounce for server API request
            loadProductsInternal(query, "", isRefresh = true)
        }
    }

    fun setSortOption(sortOption: SortOption) {
        selectedSort.value = sortOption
        applyLocalFilterAndSort()
    }

    fun setPriceRange(min: Double?, max: Double?) {
        minPrice.value = min
        maxPrice.value = max
        applyLocalFilterAndSort()
    }

    fun clearFilters() {
        selectedSort.value = SortOption.NONE
        minPrice.value = null
        maxPrice.value = null
        applyLocalFilterAndSort()
    }

    fun loadProducts(isRefresh: Boolean = false) {
        val query = searchQuery.value ?: ""
        val category = selectedCategory.value ?: ""
        loadProductsInternal(query, category, isRefresh)
    }

    private fun loadProductsInternal(query: String, category: String, isRefresh: Boolean) {
        if (isRefresh) {
            skip = 0
            isLastPage = false
            // Only show loader if raw products are completely empty, preventing flickering on instant search
            val raw = _rawProducts.value ?: emptyList()
            if (raw.isEmpty()) {
                _productsState.value = Resource.Loading
            }
        }

        if (isLastPage) return

        viewModelScope.launch {
            try {
                val response = when {
                    query.isNotEmpty() -> repository.searchProducts(query, limit, skip)
                    category.isNotEmpty() -> repository.getProductsByCategory(category, limit, skip)
                    else -> repository.getProducts(limit, skip)
                }

                val newProducts = response.products
                total = response.total
                skip += newProducts.size
                isLastPage = skip >= total

                val updatedList = if (isRefresh) newProducts else (_rawProducts.value ?: emptyList()) + newProducts
                _rawProducts.value = updatedList

                applyLocalFilterAndSort()
            } catch (e: Exception) {
                if (isRefresh) {
                    _productsState.value = Resource.Error(e.message ?: "An unexpected error occurred")
                } else {
                    _isLoadingMore.value = false
                }
            }
        }
    }

    fun loadMoreProducts() {
        if (_isLoadingMore.value == true || isLastPage || _productsState.value is Resource.Loading) return

        _isLoadingMore.value = true
        val query = searchQuery.value ?: ""
        val category = selectedCategory.value ?: ""

        viewModelScope.launch {
            try {
                val response = when {
                    query.isNotEmpty() -> repository.searchProducts(query, limit, skip)
                    category.isNotEmpty() -> repository.getProductsByCategory(category, limit, skip)
                    else -> repository.getProducts(limit, skip)
                }

                val newProducts = response.products
                total = response.total
                skip += newProducts.size
                isLastPage = skip >= total

                val updatedList = (_rawProducts.value ?: emptyList()) + newProducts
                _rawProducts.value = updatedList

                applyLocalFilterAndSort()
                _isLoadingMore.value = false
            } catch (e: Exception) {
                _isLoadingMore.value = false
            }
        }
    }

    private fun applyLocalFilterAndSort() {
        val raw = _rawProducts.value ?: emptyList()
        var filtered = raw

        // Filter locally by active search query instantly
        val query = searchQuery.value ?: ""
        if (query.isNotEmpty()) {
            filtered = filtered.filter { product ->
                product.title.contains(query, ignoreCase = true) ||
                        (product.brand?.contains(query, ignoreCase = true) == true) ||
                        product.category.contains(query, ignoreCase = true)
            }
        }

        // Filter locally by price range
        val min = minPrice.value
        val max = maxPrice.value
        if (min != null || max != null) {
            filtered = filtered.filter { product ->
                val finalPrice = product.discountedPrice
                (min == null || finalPrice >= min) && (max == null || finalPrice <= max)
            }
        }

        // Sort locally
        val sorted = when (selectedSort.value ?: SortOption.NONE) {
            SortOption.PRICE_ASC -> filtered.sortedBy { it.discountedPrice }
            SortOption.PRICE_DESC -> filtered.sortedByDescending { it.discountedPrice }
            SortOption.RATING_DESC -> filtered.sortedByDescending { it.rating }
            SortOption.NONE -> filtered
        }

        _productsState.value = Resource.Success(sorted)
    }
}

class ProductViewModelFactory(private val repository: ProductRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
