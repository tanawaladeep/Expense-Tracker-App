package com.example.taskmanagementapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskmanagementapp.data.local.TaskDatabase
import com.example.taskmanagementapp.data.model.Task
import com.example.taskmanagementapp.data.repository.TaskRepository
import com.example.taskmanagementapp.databinding.ActivityMainBinding
import com.example.taskmanagementapp.ui.adapter.TaskAdapter
import com.example.taskmanagementapp.ui.dialog.AddEditTaskDialog
import com.example.taskmanagementapp.ui.viewmodel.SortOrder
import com.example.taskmanagementapp.ui.viewmodel.TaskViewModel
import com.example.taskmanagementapp.ui.viewmodel.TaskViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: TaskViewModel
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Database, Repository, and ViewModel
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val factory = TaskViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            onCompleteToggle = { task ->
                viewModel.toggleTaskCompletion(task)
            },
            onEditClick = { task ->
                AddEditTaskDialog.newInstance(task.id).show(supportFragmentManager, "EDIT_TASK")
            },
            onDeleteClick = { task ->
                showDeleteConfirmationDialog(task)
            }
        )

        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupListeners() {
        // FAB Add Click
        binding.fabAddTask.setOnClickListener {
            AddEditTaskDialog.newInstance().show(supportFragmentManager, "ADD_TASK")
        }

        // Search Input Changes
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Category Filter Chips
        binding.chipGroupCategoryFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chipCatWork -> "WORK"
                R.id.chipCatPersonal -> "PERSONAL"
                R.id.chipCatShopping -> "SHOPPING"
                R.id.chipCatHealth -> "HEALTH"
                R.id.chipCatStudy -> "STUDY"
                R.id.chipCatOther -> "OTHER"
                else -> "ALL"
            }
            viewModel.updateCategoryFilter(selectedCategory)
        }

        // Status Filter Chips
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedStatus = when (checkedIds.firstOrNull()) {
                R.id.chipStatusPending -> "PENDING"
                R.id.chipStatusInProgress -> "IN_PROGRESS"
                R.id.chipStatusCompleted -> "COMPLETED"
                else -> "ALL"
            }
            viewModel.updateStatusFilter(selectedStatus)
        }

        // Sort Button Popup
        binding.btnSort.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.menu_sort, popup.menu)

            // Select currently active sort
            val activeSort = viewModel.sortOrder.value
            val activeItemId = when (activeSort) {
                SortOrder.DUE_DATE_ASC -> R.id.sort_due_asc
                SortOrder.DUE_DATE_DESC -> R.id.sort_due_desc
                SortOrder.PRIORITY_HIGH_TO_LOW -> R.id.sort_priority_high
                SortOrder.PRIORITY_LOW_TO_HIGH -> R.id.sort_priority_low
                SortOrder.TITLE_A_Z -> R.id.sort_title
                SortOrder.CREATED_DESC -> R.id.sort_created
            }
            popup.menu.findItem(activeItemId)?.isChecked = true

            popup.setOnMenuItemClickListener { item ->
                val order = when (item.itemId) {
                    R.id.sort_due_asc -> SortOrder.DUE_DATE_ASC
                    R.id.sort_due_desc -> SortOrder.DUE_DATE_DESC
                    R.id.sort_priority_high -> SortOrder.PRIORITY_HIGH_TO_LOW
                    R.id.sort_priority_low -> SortOrder.PRIORITY_LOW_TO_HIGH
                    R.id.sort_title -> SortOrder.TITLE_A_Z
                    R.id.sort_created -> SortOrder.CREATED_DESC
                    else -> SortOrder.DUE_DATE_ASC
                }
                viewModel.updateSortOrder(order)
                true
            }
            popup.show()
        }
    }

    private fun observeViewModel() {
        // Collect tasks Flow
        lifecycleScope.launch {
            viewModel.filteredTasks.collect { tasks ->
                adapter.submitList(tasks)
                if (tasks.isEmpty()) {
                    binding.rvTasks.visibility = View.GONE
                    binding.layoutEmptyState.visibility = View.VISIBLE
                } else {
                    binding.rvTasks.visibility = View.VISIBLE
                    binding.layoutEmptyState.visibility = View.GONE
                }
            }
        }

        // Collect stats Flow
        lifecycleScope.launch {
            viewModel.taskStats.collect { stats ->
                binding.tvStatsRatio.text = "${stats.completedTasks}/${stats.totalTasks} Tasks"

                val percentage = if (stats.totalTasks > 0) {
                    (stats.completedTasks * 100) / stats.totalTasks
                } else {
                    0
                }
                binding.progressIndicator.setProgress(percentage, true)

                binding.tvProgressHint.text = when {
                    stats.totalTasks == 0 -> "Let's get started with your tasks!"
                    percentage == 0 -> "Let's complete your first task today!"
                    percentage < 50 -> "Good start! Keep ticking them off."
                    percentage < 100 -> "Over halfway there! Keep it up."
                    else -> "Amazing! All tasks completed today! 🎉"
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTask(task)
            }
            .show()
    }
}