package com.example.expensetrackerapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddExpenseBottomSheet : BottomSheetDialogFragment() {

    interface ExpenseSaveListener {
        fun onExpenseSaved(expense: Expense)
    }

    var saveListener: ExpenseSaveListener? = null
    private var expenseToEdit: Expense? = null
    private var selectedCalendar = Calendar.getInstance()

    private lateinit var tvDialogTitle: TextView
    private lateinit var etAmount: TextInputEditText
    private lateinit var cgCategories: ChipGroup
    private lateinit var etDescription: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton

    companion object {
        private const val ARG_EXPENSE = "arg_expense"

        fun newInstance(expense: Expense? = null): AddExpenseBottomSheet {
            val fragment = AddExpenseBottomSheet()
            expense?.let {
                val args = Bundle()
                args.putSerializable(ARG_EXPENSE, it)
                fragment.arguments = args
            }
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        expenseToEdit = arguments?.getSerializable(ARG_EXPENSE) as? Expense
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_expense, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvDialogTitle = view.findViewById(R.id.tvDialogTitle)
        etAmount = view.findViewById(R.id.etAmount)
        cgCategories = view.findViewById(R.id.cgCategories)
        etDescription = view.findViewById(R.id.etDescription)
        etDate = view.findViewById(R.id.etDate)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnSave = view.findViewById(R.id.btnSave)

        // Initialize values
        if (expenseToEdit != null) {
            tvDialogTitle.text = "Edit Expense"
            val expense = expenseToEdit!!
            etAmount.setText(expense.amount.toString())
            etDescription.setText(expense.description)
            selectedCalendar.timeInMillis = expense.date
            selectCategoryChip(expense.category)
        } else {
            tvDialogTitle.text = "Add Expense"
            // Pre-select Food category as default
            cgCategories.check(R.id.chipFood)
        }

        updateDateLabel()

        // Set up Date Picker
        etDate.setOnClickListener {
            showDatePicker()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            validateAndSave()
        }
    }

    private fun showDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            selectedCalendar.set(Calendar.YEAR, year)
            selectedCalendar.set(Calendar.MONTH, month)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateLabel()
        }

        DatePickerDialog(
            requireContext(),
            dateSetListener,
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateLabel() {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        etDate.setText(sdf.format(selectedCalendar.time))
    }

    private fun getSelectedCategory(): String {
        return when (cgCategories.checkedChipId) {
            R.id.chipFood -> "Food"
            R.id.chipTravel -> "Travel"
            R.id.chipShopping -> "Shopping"
            R.id.chipBills -> "Bills"
            R.id.chipOther -> "Other"
            else -> ""
        }
    }

    private fun selectCategoryChip(category: String) {
        val chipId = when (category.lowercase()) {
            "food" -> R.id.chipFood
            "travel" -> R.id.chipTravel
            "shopping" -> R.id.chipShopping
            "bills" -> R.id.chipBills
            else -> R.id.chipOther
        }
        cgCategories.check(chipId)
    }

    private fun validateAndSave() {
        val amountStr = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val category = getSelectedCategory()

        if (amountStr.isEmpty()) {
            etAmount.error = "Please enter an amount"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            etAmount.error = "Please enter a valid positive amount"
            return
        }

        if (category.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        if (description.isEmpty()) {
            etDescription.error = "Please enter a description"
            return
        }

        val updatedExpense = Expense(
            id = expenseToEdit?.id,
            amount = amount,
            category = category,
            description = description,
            date = selectedCalendar.timeInMillis
        )

        saveListener?.onExpenseSaved(updatedExpense)
        dismiss()
    }
}
