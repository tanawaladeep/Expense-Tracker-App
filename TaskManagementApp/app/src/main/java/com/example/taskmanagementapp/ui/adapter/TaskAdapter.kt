package com.example.taskmanagementapp.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Paint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagementapp.R
import com.example.taskmanagementapp.data.model.Task
import com.example.taskmanagementapp.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onCompleteToggle: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context
            val isCompleted = task.status.uppercase() == "COMPLETED"

            // Bind text
            binding.tvTaskTitle.text = task.title
            binding.tvTaskDesc.text = task.description

            // Apply strikethrough if completed
            if (isCompleted) {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTaskTitle.setTextColor(getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
                binding.tvTaskDesc.paintFlags = binding.tvTaskDesc.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvTaskDesc.setTextColor(getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            } else {
                binding.tvTaskTitle.paintFlags = binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTaskTitle.setTextColor(getThemeColor(context, com.google.android.material.R.attr.colorOnSurface))
                binding.tvTaskDesc.paintFlags = binding.tvTaskDesc.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvTaskDesc.setTextColor(getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            }

            // Bind checkbox state
            binding.cbComplete.setOnCheckedChangeListener(null)
            binding.cbComplete.isChecked = isCompleted
            binding.cbComplete.setOnCheckedChangeListener { _, _ ->
                onCompleteToggle(task)
            }

            // Bind due date
            val (dateText, dateColor) = formatDueDate(task.dueDate, isCompleted, context)
            binding.tvDueDate.text = dateText
            binding.tvDueDate.setTextColor(dateColor)
            binding.ivCalendar.imageTintList = ColorStateList.valueOf(dateColor)

            // Bind priority indicator (bar & badge)
            val isDark = isDarkMode(context)
            val priorityColor = when (task.priority.uppercase()) {
                "HIGH" -> ContextCompat.getColor(context, R.color.priority_high)
                "MEDIUM" -> ContextCompat.getColor(context, R.color.priority_medium)
                else -> ContextCompat.getColor(context, R.color.priority_low)
            }
            binding.viewPriorityIndicator.setBackgroundColor(priorityColor)

            val priorityBg = when (task.priority.uppercase()) {
                "HIGH" -> ContextCompat.getColor(context, if (isDark) R.color.priority_high_bg_dark else R.color.priority_high_bg)
                "MEDIUM" -> ContextCompat.getColor(context, if (isDark) R.color.priority_medium_bg_dark else R.color.priority_medium_bg)
                else -> ContextCompat.getColor(context, if (isDark) R.color.priority_low_bg_dark else R.color.priority_low_bg)
            }
            binding.tvPriorityBadge.text = task.priority.uppercase()
            binding.tvPriorityBadge.setTextColor(priorityColor)
            binding.tvPriorityBadge.backgroundTintList = ColorStateList.valueOf(priorityBg)

            // Bind category badge
            val categoryColor = when (task.category.uppercase()) {
                "WORK" -> ContextCompat.getColor(context, R.color.cat_work)
                "PERSONAL" -> ContextCompat.getColor(context, R.color.cat_personal)
                "SHOPPING" -> ContextCompat.getColor(context, R.color.cat_shopping)
                "HEALTH" -> ContextCompat.getColor(context, R.color.cat_health)
                "STUDY" -> ContextCompat.getColor(context, R.color.cat_study)
                else -> ContextCompat.getColor(context, R.color.cat_other)
            }
            val categoryBg = when (task.category.uppercase()) {
                "WORK" -> ContextCompat.getColor(context, if (isDark) R.color.dark_primary_container else R.color.cat_work_bg)
                "PERSONAL" -> ContextCompat.getColor(context, if (isDark) R.color.dark_tertiary_container else R.color.cat_personal_bg)
                "SHOPPING" -> ContextCompat.getColor(context, if (isDark) R.color.dark_secondary_container else R.color.cat_shopping_bg)
                "HEALTH" -> ContextCompat.getColor(context, if (isDark) R.color.status_completed_bg else R.color.cat_health_bg)
                "STUDY" -> ContextCompat.getColor(context, if (isDark) R.color.dark_tertiary_container else R.color.cat_study_bg)
                else -> ContextCompat.getColor(context, if (isDark) R.color.dark_surface_variant else R.color.cat_other_bg)
            }
            binding.tvCategoryBadge.text = task.category.uppercase()
            binding.tvCategoryBadge.setTextColor(categoryColor)
            binding.tvCategoryBadge.backgroundTintList = ColorStateList.valueOf(categoryBg)

            // Bind action buttons
            binding.btnEdit.setOnClickListener { onEditClick(task) }
            binding.btnDelete.setOnClickListener { onDeleteClick(task) }
        }

        private fun formatDueDate(dueDateMs: Long, isCompleted: Boolean, context: Context): Pair<String, Int> {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val taskDate = Calendar.getInstance().apply {
                timeInMillis = dueDateMs
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val diffDays = (taskDate - today) / (1000 * 60 * 60 * 24)
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(dueDateMs))

            return when {
                isCompleted -> Pair("Completed", getThemeColor(context, com.google.android.material.R.attr.colorSecondary))
                diffDays < 0 -> Pair("Overdue: $formattedDate", ContextCompat.getColor(context, R.color.priority_high))
                diffDays == 0L -> Pair("Today", ContextCompat.getColor(context, R.color.priority_medium))
                diffDays == 1L -> Pair("Tomorrow", getThemeColor(context, androidx.appcompat.R.attr.colorPrimary))
                else -> Pair(formattedDate, getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant))
            }
        }

        private fun getThemeColor(context: Context, attrRes: Int): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attrRes, typedValue, true)
            return typedValue.data
        }

        private fun isDarkMode(context: Context): Boolean {
            return (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
}

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem
    }
}
