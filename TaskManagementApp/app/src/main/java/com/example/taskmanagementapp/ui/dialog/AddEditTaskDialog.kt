package com.example.taskmanagementapp.ui.dialog

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.taskmanagementapp.R
import com.example.taskmanagementapp.data.local.TaskDatabase
import com.example.taskmanagementapp.data.model.Task
import com.example.taskmanagementapp.data.repository.TaskRepository
import com.example.taskmanagementapp.databinding.DialogAddEditTaskBinding
import com.example.taskmanagementapp.ui.viewmodel.TaskViewModel
import com.example.taskmanagementapp.ui.viewmodel.TaskViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddEditTaskDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddEditTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TaskViewModel
    private var taskId: Int = -1
    private var selectedDueDateMs: Long? = null

    companion object {
        private const val ARG_TASK_ID = "arg_task_id"

        fun newInstance(taskId: Int = -1): AddEditTaskDialog {
            val fragment = AddEditTaskDialog()
            val args = Bundle().apply {
                putInt(ARG_TASK_ID, taskId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        taskId = arguments?.getInt(ARG_TASK_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel shared with Activity
        val database = TaskDatabase.getDatabase(requireContext())
        val repository = TaskRepository(database.taskDao())
        val factory = TaskViewModelFactory(repository)
        viewModel = ViewModelProvider(requireActivity(), factory)[TaskViewModel::class.java]

        setupDatePicker()

        if (taskId != -1) {
            // Edit Mode: Load task details
            binding.tvDialogTitle.text = "Edit Task"
            binding.btnSaveTask.text = "Update Task"
            loadTaskDetails()
        } else {
            // Add Mode: Initialize defaults
            binding.tvDialogTitle.text = "New Task"
            binding.btnSaveTask.text = "Save Task"
            setDefaultDueDate()
            binding.togglePriorityGroup.check(R.id.btnPriorityLow)
            binding.chipGroupCategory.check(R.id.chipOther)
        }

        binding.btnSaveTask.setOnClickListener {
            saveTask()
        }
    }

    private fun setupDatePicker() {
        binding.etDueDate.setOnClickListener {
            val datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Due Date")
                .setSelection(selectedDueDateMs ?: MaterialDatePicker.todayInUtcMilliseconds())
                .build()

            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDueDateMs = selection
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                binding.etDueDate.setText(dateFormat.format(Date(selection)))
            }

            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setDefaultDueDate() {
        val calendar = Calendar.getInstance()
        // Default to today
        selectedDueDateMs = calendar.timeInMillis
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.etDueDate.setText(dateFormat.format(calendar.time))
    }

    private fun loadTaskDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            val task = viewModel.filteredTasks.value.find { it.id == taskId }
            task?.let {
                binding.etTitle.setText(it.title)
                binding.etDesc.setText(it.description)

                selectedDueDateMs = it.dueDate
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                binding.etDueDate.setText(dateFormat.format(Date(it.dueDate)))

                // Set priority
                val priorityBtnId = when (it.priority.uppercase()) {
                    "HIGH" -> R.id.btnPriorityHigh
                    "MEDIUM" -> R.id.btnPriorityMedium
                    else -> R.id.btnPriorityLow
                }
                binding.togglePriorityGroup.check(priorityBtnId)

                // Set category
                val categoryChipId = when (it.category.uppercase()) {
                    "WORK" -> R.id.chipWork
                    "PERSONAL" -> R.id.chipPersonal
                    "SHOPPING" -> R.id.chipShopping
                    "HEALTH" -> R.id.chipHealth
                    "STUDY" -> R.id.chipStudy
                    else -> R.id.chipOther
                }
                binding.chipGroupCategory.check(categoryChipId)
            }
        }
    }

    private fun saveTask() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDesc.text.toString().trim()

        if (title.isEmpty()) {
            binding.tilTitle.error = "Title is required"
            return
        } else {
            binding.tilTitle.error = null
        }

        val priority = when (binding.togglePriorityGroup.checkedButtonId) {
            R.id.btnPriorityHigh -> "HIGH"
            R.id.btnPriorityMedium -> "MEDIUM"
            else -> "LOW"
        }

        val category = when (binding.chipGroupCategory.checkedChipId) {
            R.id.chipWork -> "WORK"
            R.id.chipPersonal -> "PERSONAL"
            R.id.chipShopping -> "SHOPPING"
            R.id.chipHealth -> "HEALTH"
            R.id.chipStudy -> "STUDY"
            else -> "OTHER"
        }

        val dueDate = selectedDueDateMs ?: System.currentTimeMillis()

        if (taskId != -1) {
            // Update existing
            viewLifecycleOwner.lifecycleScope.launch {
                val existingTask = viewModel.filteredTasks.value.find { it.id == taskId }
                existingTask?.let {
                    val updatedTask = it.copy(
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        priority = priority,
                        category = category
                    )
                    viewModel.updateTask(updatedTask)
                }
            }
        } else {
            // Create new
            val newTask = Task(
                title = title,
                description = description,
                dueDate = dueDate,
                priority = priority,
                category = category,
                status = "PENDING"
            )
            viewModel.insertTask(newTask)
        }

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
