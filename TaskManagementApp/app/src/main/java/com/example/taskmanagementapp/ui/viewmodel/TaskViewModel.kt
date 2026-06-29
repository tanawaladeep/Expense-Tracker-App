package com.example.taskmanagementapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.taskmanagementapp.data.model.Task
import com.example.taskmanagementapp.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortOrder {
    DUE_DATE_ASC,
    DUE_DATE_DESC,
    PRIORITY_HIGH_TO_LOW,
    PRIORITY_LOW_TO_HIGH,
    TITLE_A_Z,
    CREATED_DESC
}

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val statusFilter = MutableStateFlow("ALL")
    val categoryFilter = MutableStateFlow("ALL")
    val sortOrder = MutableStateFlow(SortOrder.DUE_DATE_ASC)

    // Flow for the filtered and sorted list of tasks
    val filteredTasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        searchQuery,
        statusFilter,
        categoryFilter,
        sortOrder
    ) { tasks, query, status, category, sort ->
        tasks.filter { task ->
            val matchesQuery = task.title.contains(query, ignoreCase = true) ||
                               task.description.contains(query, ignoreCase = true)
            val matchesStatus = status == "ALL" || task.status.equals(status, ignoreCase = true)
            val matchesCategory = category == "ALL" || task.category.equals(category, ignoreCase = true)
            matchesQuery && matchesStatus && matchesCategory
        }.sortedWith { t1, t2 ->
            when (sort) {
                SortOrder.DUE_DATE_ASC -> t1.dueDate.compareTo(t2.dueDate)
                SortOrder.DUE_DATE_DESC -> t2.dueDate.compareTo(t1.dueDate)
                SortOrder.PRIORITY_HIGH_TO_LOW -> getPriorityWeight(t2.priority) - getPriorityWeight(t1.priority)
                SortOrder.PRIORITY_LOW_TO_HIGH -> getPriorityWeight(t1.priority) - getPriorityWeight(t2.priority)
                SortOrder.TITLE_A_Z -> t1.title.compareTo(t2.title, ignoreCase = true)
                SortOrder.CREATED_DESC -> t2.createdAt.compareTo(t1.createdAt)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow for overall database stats (total tasks vs completed tasks)
    val taskStats: StateFlow<TaskStats> = repository.allTasks.map { tasks ->
        val total = tasks.size
        val completed = tasks.count { it.status.uppercase() == "COMPLETED" }
        TaskStats(total, completed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskStats(0, 0))

    fun insertTask(task: Task) = viewModelScope.launch {
        repository.insert(task)
    }

    fun updateTask(task: Task) = viewModelScope.launch {
        repository.update(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.delete(task)
    }

    fun toggleTaskCompletion(task: Task) = viewModelScope.launch {
        val newStatus = if (task.status.uppercase() == "COMPLETED") "PENDING" else "COMPLETED"
        repository.update(task.copy(status = newStatus))
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateStatusFilter(status: String) {
        statusFilter.value = status
    }

    fun updateCategoryFilter(category: String) {
        categoryFilter.value = category
    }

    fun updateSortOrder(order: SortOrder) {
        sortOrder.value = order
    }

    private fun getPriorityWeight(priority: String): Int {
        return when (priority.uppercase()) {
            "HIGH" -> 3
            "MEDIUM" -> 2
            "LOW" -> 1
            else -> 0
        }
    }
}

data class TaskStats(val totalTasks: Int, val completedTasks: Int)

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
