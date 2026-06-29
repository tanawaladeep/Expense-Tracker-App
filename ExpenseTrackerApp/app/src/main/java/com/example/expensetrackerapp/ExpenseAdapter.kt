package com.example.expensetrackerapp

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onItemClickListener: OnItemClickListener
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(expense: Expense)
        fun onDeleteClick(expense: Expense)
    }

    inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardExpense: View = itemView.findViewById(R.id.cardExpense)
        val iconContainer: FrameLayout = itemView.findViewById(R.id.iconContainer)
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val expense = expenses[position]
        val context = holder.itemView.context

        // Bind data
        holder.tvDescription.text = expense.description
        
        // Format Date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.tvDate.text = dateFormat.format(Date(expense.date))

        // Format Amount
        holder.tvAmount.text = String.format("-₹%.2f", expense.amount)

        // Apply Category UI Styles
        val config = getCategoryConfig(expense.category)
        
        holder.iconContainer.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, config.bgResId)
        )
        holder.ivCategoryIcon.setImageResource(config.iconResId)
        holder.ivCategoryIcon.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(context, config.accentResId)
        )

        // Click Listener for Edit
        holder.cardExpense.setOnClickListener {
            onItemClickListener.onItemClick(expense)
        }

        holder.btnDelete.setOnClickListener {
            onItemClickListener.onDeleteClick(expense)
        }
    }

    override fun getItemCount(): Int = expenses.size

    fun updateData(newExpenses: List<Expense>) {
        this.expenses = newExpenses
        notifyDataSetChanged()
    }

    fun getExpenseAt(position: Int): Expense {
        return expenses[position]
    }

    private data class CategoryUiConfig(
        val bgResId: Int,
        val iconResId: Int,
        val accentResId: Int
    )

    private fun getCategoryConfig(category: String): CategoryUiConfig {
        return when (category.lowercase()) {
            "food" -> CategoryUiConfig(R.color.cat_food_bg, R.drawable.ic_food, R.color.cat_food_accent)
            "travel" -> CategoryUiConfig(R.color.cat_travel_bg, R.drawable.ic_travel, R.color.cat_travel_accent)
            "shopping" -> CategoryUiConfig(R.color.cat_shopping_bg, R.drawable.ic_shopping, R.color.cat_shopping_accent)
            "bills" -> CategoryUiConfig(R.color.cat_bills_bg, R.drawable.ic_bills, R.color.cat_bills_accent)
            else -> CategoryUiConfig(R.color.cat_other_bg, R.drawable.ic_other, R.color.cat_other_accent)
        }
    }
}
