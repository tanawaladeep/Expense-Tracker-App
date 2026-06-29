package com.example.expensetrackerapp

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), ExpenseAdapter.OnItemClickListener, AddExpenseBottomSheet.ExpenseSaveListener {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ExpenseAdapter

    private lateinit var tvTotalSpending: TextView
    private lateinit var layoutCategoryBreakdown: LinearLayout
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var rvExpenses: RecyclerView
    private lateinit var fabAddExpense: FloatingActionButton
    private lateinit var toolbar: Toolbar

    private var expensesList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load Theme Preference before super.onCreate
        val sharedPrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false)
        val expectedMode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (AppCompatDelegate.getDefaultNightMode() != expectedMode) {
            AppCompatDelegate.setDefaultNightMode(expectedMode)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Database
        dbHelper = DatabaseHelper(this)

        // Bind Views
        toolbar = findViewById(R.id.toolbar)
        tvTotalSpending = findViewById(R.id.tvTotalSpending)
        layoutCategoryBreakdown = findViewById(R.id.layoutCategoryBreakdown)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        rvExpenses = findViewById(R.id.rvExpenses)
        fabAddExpense = findViewById(R.id.fabAddExpense)

        setSupportActionBar(toolbar)

        // Set up RecyclerView
        rvExpenses.layoutManager = LinearLayoutManager(this)
        adapter = ExpenseAdapter(expensesList, this)
        rvExpenses.adapter = adapter

        // Setup Swipe-to-Delete
        setupSwipeToDelete()

        // FAB Click
        fabAddExpense.setOnClickListener {
            val bottomSheet = AddExpenseBottomSheet.newInstance()
            bottomSheet.saveListener = this
            bottomSheet.show(supportFragmentManager, "AddExpenseBottomSheet")
        }

        // Load data
        loadExpenses()
    }

    private fun loadExpenses() {
        expensesList.clear()
        expensesList.addAll(dbHelper.getAllExpenses())
        adapter.updateData(expensesList)
        updateStatistics(expensesList)
    }

    private fun updateStatistics(expenses: List<Expense>) {
        val grandTotal = expenses.sumOf { it.amount }
        tvTotalSpending.text = String.format("₹%.2f", grandTotal)

        layoutCategoryBreakdown.removeAllViews()

        if (expenses.isEmpty()) {
            val tvStatsEmpty = TextView(this).apply {
                text = "No data available yet"
                setTextColor(Color.parseColor("#B3FFFFFF"))
                textSize = 12f
                typeface = android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.ITALIC)
            }
            layoutCategoryBreakdown.addView(tvStatsEmpty)
            layoutEmptyState.visibility = View.VISIBLE
            rvExpenses.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvExpenses.visibility = View.VISIBLE

            val totalsByCategory = expenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            for ((category, amount) in totalsByCategory) {
                val percentage = (amount / grandTotal * 100).toInt()

                // Vertical container for spacing
                val catLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(0, 0, 0, dpToPx(10))
                }

                // Header (Name and Info)
                val headerLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val tvName = TextView(this).apply {
                    text = category
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val tvInfo = TextView(this).apply {
                    text = String.format("₹%.2f (%d%%)", amount, percentage)
                    setTextColor(Color.parseColor("#E0E7FF"))
                    textSize = 13f
                    typeface = android.graphics.Typeface.DEFAULT
                }

                headerLayout.addView(tvName)
                headerLayout.addView(tvInfo)

                // Progress Bar
                val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100
                    progress = percentage
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(6)
                    ).apply {
                        setMargins(0, dpToPx(4), 0, 0)
                    }
                    // Custom style
                    progressTintList = ColorStateList.valueOf(Color.WHITE)
                    progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
                }

                catLayout.addView(headerLayout)
                catLayout.addView(progressBar)

                layoutCategoryBreakdown.addView(catLayout)
            }
        }
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val expenseToDelete = adapter.getExpenseAt(position)
                
                // Delete from DB
                dbHelper.deleteExpense(expenseToDelete.id!!)
                
                // Reload data
                loadExpenses()

                // Show Snackbar undo option
                val snackbar = Snackbar.make(
                    rvExpenses,
                    "Expense deleted: ${expenseToDelete.description}",
                    Snackbar.LENGTH_LONG
                )
                snackbar.setAction("UNDO") {
                    dbHelper.addExpense(expenseToDelete)
                    loadExpenses()
                }
                snackbar.setActionTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent))
                snackbar.show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                if (dX < 0) {
                    // Draw red background
                    val background = ColorDrawable()
                    background.color = ContextCompat.getColor(this@MainActivity, R.color.swipe_delete_bg)
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)

                    // Draw trash icon
                    val deleteIcon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
                    val iconWidth = deleteIcon?.intrinsicWidth ?: 0
                    val iconHeight = deleteIcon?.intrinsicHeight ?: 0

                    val deleteIconTop = itemView.top + (itemHeight - iconHeight) / 2
                    val deleteIconMargin = (itemHeight - iconHeight) / 2
                    val deleteIconLeft = itemView.right - deleteIconMargin - iconWidth
                    val deleteIconRight = itemView.right - deleteIconMargin
                    val deleteIconBottom = deleteIconTop + iconHeight

                    deleteIcon?.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
                    deleteIcon?.setTint(Color.WHITE)
                    deleteIcon?.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(rvExpenses)
    }

    // Edit item click
    override fun onItemClick(expense: Expense) {
        val bottomSheet = AddExpenseBottomSheet.newInstance(expense)
        bottomSheet.saveListener = this
        bottomSheet.show(supportFragmentManager, "AddExpenseBottomSheet")
    }

    override fun onDeleteClick(expense: Expense) {
        // Delete from DB
        dbHelper.deleteExpense(expense.id!!)
        
        // Reload data
        loadExpenses()

        // Show Snackbar undo option
        val snackbar = Snackbar.make(
            rvExpenses,
            "Expense deleted: ${expense.description}",
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction("UNDO") {
            dbHelper.addExpense(expense)
            loadExpenses()
        }
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.accent))
        snackbar.show()
    }

    // Save/Edit Dialog Callback
    override fun onExpenseSaved(expense: Expense) {
        if (expense.id != null) {
            // Update
            dbHelper.updateExpense(expense)
        } else {
            // Insert
            dbHelper.addExpense(expense)
        }
        loadExpenses()
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val toggleItem = menu.findItem(R.id.action_toggle_theme)
        val sharedPrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false)
        if (isDarkMode) {
            toggleItem.setIcon(R.drawable.ic_light_mode)
            toggleItem.icon?.setTint(Color.WHITE)
        } else {
            toggleItem.setIcon(R.drawable.ic_dark_mode)
            toggleItem.icon?.setTint(ContextCompat.getColor(this, R.color.text_primary_light))
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_toggle_theme) {
            val sharedPrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
            val isDarkMode = sharedPrefs.getBoolean("is_dark_mode", false)
            
            // Toggle
            sharedPrefs.edit().putBoolean("is_dark_mode", !isDarkMode).apply()
            
            // Apply immediately
            if (!isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}