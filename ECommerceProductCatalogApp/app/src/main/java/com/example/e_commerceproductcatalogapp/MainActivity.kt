package com.example.e_commerceproductcatalogapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.e_commerceproductcatalogapp.data.api.RetrofitClient
import com.example.e_commerceproductcatalogapp.data.model.Product
import com.example.e_commerceproductcatalogapp.data.repository.ProductRepository
import com.example.e_commerceproductcatalogapp.databinding.ActivityMainBinding
import com.example.e_commerceproductcatalogapp.ui.activity.ProductDetailActivity
import com.example.e_commerceproductcatalogapp.ui.adapter.CategoryChipAdapter
import com.example.e_commerceproductcatalogapp.ui.adapter.ProductAdapter
import com.example.e_commerceproductcatalogapp.ui.dialog.FilterBottomSheetDialog
import com.example.e_commerceproductcatalogapp.ui.viewmodel.ProductViewModel
import com.example.e_commerceproductcatalogapp.ui.viewmodel.ProductViewModelFactory
import com.example.e_commerceproductcatalogapp.ui.viewmodel.Resource

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ProductViewModel
    private lateinit var productAdapter: ProductAdapter
    private lateinit var categoryChipAdapter: CategoryChipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = ProductRepository(RetrofitClient.productService)
        val factory = ProductViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ProductViewModel::class.java]

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        productAdapter = ProductAdapter { product ->
            val intent = Intent(this, ProductDetailActivity::class.java).apply {
                putExtra("EXTRA_PRODUCT", product)
            }
            startActivity(intent)
        }
        productAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = productAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }

        categoryChipAdapter = CategoryChipAdapter { category ->
            viewModel.setCategory(category)
        }
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.HORIZONTAL, false)
            adapter = categoryChipAdapter
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadProducts(isRefresh = true)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.fetchCategories()
            viewModel.loadProducts(isRefresh = true)
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.rvProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) { // Only paginate when scrolling down
                    val layoutManager = recyclerView.layoutManager as GridLayoutManager
                    val totalItemCount = layoutManager.itemCount
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                    if (lastVisibleItemPosition >= totalItemCount - 4) {
                        viewModel.loadMoreProducts()
                    }
                }
            }
        })

        binding.fabFilter.setOnClickListener { showFilterDialog() }
        binding.btnMainFilter.setOnClickListener { showFilterDialog() }
    }

    private fun showFilterDialog() {
        val dialog = FilterBottomSheetDialog(viewModel)
        dialog.show(supportFragmentManager, "FilterDialog")
    }

    private fun observeViewModel() {
        viewModel.productsState.observe(this) { state ->
            binding.swipeRefresh.isRefreshing = false
            when (state) {
                is Resource.Loading -> {
                    if (viewModel.isLoadingMore.value != true) {
                        binding.shimmerView.visibility = View.VISIBLE
                        binding.shimmerView.startShimmer()
                        binding.rvProducts.visibility = View.GONE
                        binding.errorLayout.visibility = View.GONE
                    }
                }
                is Resource.Success -> {
                    binding.shimmerView.stopShimmer()
                    binding.shimmerView.visibility = View.GONE
                    binding.rvProducts.visibility = View.VISIBLE
                    binding.errorLayout.visibility = View.GONE
                    productAdapter.submitList(state.data) {
                        if (viewModel.isLoadingMore.value != true) {
                            binding.rvProducts.scrollToPosition(0)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.shimmerView.stopShimmer()
                    binding.shimmerView.visibility = View.GONE
                    binding.rvProducts.visibility = View.GONE
                    binding.errorLayout.visibility = View.VISIBLE
                    binding.tvErrorMsg.text = state.message
                }
            }
        }

        viewModel.categoriesState.observe(this) { state ->
            if (state is Resource.Success) {
                categoryChipAdapter.submitList(state.data)
            }
        }

        viewModel.isLoadingMore.observe(this) { isLoading ->
            binding.pagingProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.selectedCategory.observe(this) { category ->
            categoryChipAdapter.setSelectedCategory(category)
        }

        viewModel.searchQuery.observe(this) { query ->
            if (binding.etSearch.text.toString() != query) {
                binding.etSearch.setText(query)
            }
        }
    }
}