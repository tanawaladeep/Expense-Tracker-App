package com.example.notesmanagementapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notesmanagementapp.R
import com.example.notesmanagementapp.data.model.Note
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(private val onNoteClick: (Note) -> Unit) :
    ListAdapter<Note, NoteAdapter.NoteViewHolder>(NoteDiffCallback()) {

    private var lastPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
        setAnimation(holder.itemView, position)
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (position > lastPosition) {
            viewToAnimate.translationY = 150f
            viewToAnimate.alpha = 0f
            viewToAnimate.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(400)
                .setStartDelay(position * 20L)
                .start()
            lastPosition = position
        }
    }

    override fun onViewDetachedFromWindow(holder: NoteViewHolder) {
        holder.itemView.clearAnimation()
        super.onViewDetachedFromWindow(holder)
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: android.widget.TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val tvContent: android.widget.TextView = itemView.findViewById(R.id.tvNoteContent)
        private val tvCategory: android.widget.TextView = itemView.findViewById(R.id.tvNoteCategory)
        private val tvDate: android.widget.TextView = itemView.findViewById(R.id.tvNoteDate)
        private val ivPin: android.widget.ImageView = itemView.findViewById(R.id.ivPinIndicator)
        private val card: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.noteCard)

        fun bind(note: Note) {
            tvTitle.text = note.title
            tvContent.text = note.content
            tvCategory.text = note.category
            
            // Format timestamp
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            tvDate.text = sdf.format(Date(note.timestamp))

            // Show pin indicator
            ivPin.visibility = if (note.isPinned) View.VISIBLE else View.GONE

            // Resolve dynamic background color based on colorIndex
            val colorRes = when (note.colorIndex) {
                0 -> R.color.note_color_default
                1 -> R.color.note_color_red
                2 -> R.color.note_color_orange
                3 -> R.color.note_color_yellow
                4 -> R.color.note_color_green
                5 -> R.color.note_color_blue
                6 -> R.color.note_color_purple
                7 -> R.color.note_color_pink
                else -> R.color.note_color_default
            }
            card.setCardBackgroundColor(ContextCompat.getColor(itemView.context, colorRes))

            itemView.setOnClickListener {
                onNoteClick(note)
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
