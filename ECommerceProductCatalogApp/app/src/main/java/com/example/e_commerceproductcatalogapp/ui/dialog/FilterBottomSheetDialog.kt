package com.example.e_commerceproductcatalogapp.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.e_commerceproductcatalogapp.R
import com.example.e_commerceproductcatalogapp.databinding.DialogFilterBinding
import com.example.e_commerceproductcatalogapp.ui.viewmodel.ProductViewModel
import com.example.e_commerceproductcatalogapp.ui.viewmodel.SortOption
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheetDialog(private val viewModel: ProductViewModel) : BottomSheetDialogFragment() {

    private var _binding: DialogFilterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        restoreStates()

        binding.btnApply.setOnClickListener {
            val minPriceStr = binding.etMinPrice.text.toString()
            val maxPriceStr = binding.etMaxPrice.text.toString()

            val minPrice = minPriceStr.toDoubleOrNull()
            val maxPrice = maxPriceStr.toDoubleOrNull()

            viewModel.setPriceRange(minPrice, maxPrice)

            val selectedSort = when (binding.sortRadioGroup.checkedRadioButtonId) {
                R.id.radioPriceAsc -> SortOption.PRICE_ASC
                R.id.radioPriceDesc -> SortOption.PRICE_DESC
                R.id.radioRatingDesc -> SortOption.RATING_DESC
                else -> SortOption.NONE
            }
            viewModel.setSortOption(selectedSort)

            dismiss()
        }

        binding.btnReset.setOnClickListener {
            binding.etMinPrice.setText("")
            binding.etMaxPrice.setText("")
            binding.sortRadioGroup.check(R.id.radioNone)
            viewModel.clearFilters()
            dismiss()
        }
    }

    private fun restoreStates() {
        val activeMin = viewModel.minPrice.value
        val activeMax = viewModel.maxPrice.value
        if (activeMin != null) binding.etMinPrice.setText(activeMin.toString())
        if (activeMax != null) binding.etMaxPrice.setText(activeMax.toString())

        val activeSort = viewModel.selectedSort.value ?: SortOption.NONE
        val checkId = when (activeSort) {
            SortOption.PRICE_ASC -> R.id.radioPriceAsc
            SortOption.PRICE_DESC -> R.id.radioPriceDesc
            SortOption.RATING_DESC -> R.id.radioRatingDesc
            SortOption.NONE -> R.id.radioNone
        }
        binding.sortRadioGroup.check(checkId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
