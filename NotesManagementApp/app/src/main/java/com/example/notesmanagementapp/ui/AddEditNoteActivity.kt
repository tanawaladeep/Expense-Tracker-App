package com.example.notesmanagementapp.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notesmanagementapp.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddEditNoteActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var tvTimestamp: TextView
    private lateinit var tvEditorTitle: TextView
    private lateinit var btnPin: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var chipGroup: ChipGroup
    private lateinit var rootView: View

    private var noteId: Int = -1
    private var isPinned: Boolean = false
    private var selectedColorIndex: Int = 0
    private var selectedCategory: String = "Personal"
    private var initialTimestamp: Long = 0L

    private lateinit var colorViews: List<ImageView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_edit_note)

        rootView = findViewById(R.id.editorRoot)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.editorRoot)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = if (imeInsets.bottom > 0) imeInsets.bottom else systemBars.bottom
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

        // Initialize UI Views
        etTitle = findViewById(R.id.etNoteTitle)
        etContent = findViewById(R.id.etNoteContent)
        tvTimestamp = findViewById(R.id.tvNoteTimestamp)
        tvEditorTitle = findViewById(R.id.tvEditorTitle)
        btnPin = findViewById(R.id.btnPin)
        btnDelete = findViewById(R.id.btnDelete)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        chipGroup = findViewById(R.id.chipGroupEditorCategories)

        // Color circles array
        colorViews = listOf(
            findViewById(R.id.color0),
            findViewById(R.id.color1),
            findViewById(R.id.color2),
            findViewById(R.id.color3),
            findViewById(R.id.color4),
            findViewById(R.id.color5),
            findViewById(R.id.color6),
            findViewById(R.id.color7)
        )

        // Read extras from Intent
        noteId = intent.getIntExtra(EXTRA_ID, -1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val content = intent.getStringExtra(EXTRA_CONTENT) ?: ""
        selectedColorIndex = intent.getIntExtra(EXTRA_COLOR_INDEX, 0)
        isPinned = intent.getBooleanExtra(EXTRA_IS_PINNED, false)
        selectedCategory = intent.getStringExtra(EXTRA_CATEGORY) ?: "Personal"
        initialTimestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())

        if (noteId != -1) {
            // Edit mode
            tvEditorTitle.text = "Edit Note"
            etTitle.setText(title)
            etContent.setText(content)
            btnDelete.visibility = View.VISIBLE
            
            // Format existing date
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            tvTimestamp.text = "Edited ${sdf.format(Date(initialTimestamp))}"
        } else {
            // Create mode
            tvEditorTitle.text = "New Note"
            btnDelete.visibility = View.GONE
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            tvTimestamp.text = "Edited ${sdf.format(Date(System.currentTimeMillis()))}"
        }

        // Initialize Pin Status UI
        updatePinIcon()

        // Initialize Background Color
        rootView.setBackgroundColor(getNoteColor(selectedColorIndex))
        selectColor(selectedColorIndex)

        // Initialize Category chip
        setCategoryChipChecked(selectedCategory)

        // Setup Color Pickers
        colorViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectColor(index)
            }
        }

        // Setup Listeners
        btnPin.setOnClickListener {
            isPinned = !isPinned
            updatePinIcon()
            val message = if (isPinned) "Note pinned" else "Note unpinned"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        btnDelete.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra(EXTRA_ID, noteId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_CONTENT, content)
                putExtra(EXTRA_COLOR_INDEX, selectedColorIndex)
                putExtra(EXTRA_IS_PINNED, isPinned)
                putExtra(EXTRA_CATEGORY, selectedCategory)
            }
            setResult(RESULT_DELETED, resultIntent)
            finish()
        }

        btnSave.setOnClickListener {
            saveNote()
        }

        btnBack.setOnClickListener {
            saveNote()
        }
        
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                selectedCategory = chip.text.toString()
            }
        }

        // Handle modern Back Press Callback
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveNote()
            }
        })
    }

    private fun saveNote() {
        val titleText = etTitle.text.toString().trim()
        val contentText = etContent.text.toString().trim()

        if (titleText.isEmpty() && contentText.isEmpty()) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val resultIntent = Intent().apply {
            putExtra(EXTRA_ID, noteId)
            putExtra(EXTRA_TITLE, titleText)
            putExtra(EXTRA_CONTENT, contentText)
            putExtra(EXTRA_COLOR_INDEX, selectedColorIndex)
            putExtra(EXTRA_IS_PINNED, isPinned)
            putExtra(EXTRA_CATEGORY, selectedCategory)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun updatePinIcon() {
        if (isPinned) {
            btnPin.setImageResource(R.drawable.ic_pin_filled)
            btnPin.setColorFilter(ContextCompat.getColor(this, R.color.primary))
        } else {
            btnPin.setImageResource(R.drawable.ic_pin)
            btnPin.setColorFilter(ContextCompat.getColor(this, R.color.black))
        }
    }

    private fun getNoteColor(colorIndex: Int): Int {
        val colorRes = when (colorIndex) {
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
        return ContextCompat.getColor(this, colorRes)
    }

    private fun selectColor(index: Int) {
        selectedColorIndex = index
        val newColor = getNoteColor(index)

        val currentBg = (rootView.background as? ColorDrawable)?.color ?: Color.WHITE
        ValueAnimator.ofObject(ArgbEvaluator(), currentBg, newColor).apply {
            duration = 300
            addUpdateListener { animator ->
                rootView.setBackgroundColor(animator.animatedValue as Int)
            }
            start()
        }

        for (i in 0..7) {
            val colorView = colorViews[i]
            val targetScale = if (i == index) 1.2f else 1.0f
            colorView.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(200)
                .start()
        }
    }

    private fun setCategoryChipChecked(category: String) {
        val chipId = when (category) {
            "Personal" -> R.id.chipEditorPersonal
            "Work" -> R.id.chipEditorWork
            "Ideas" -> R.id.chipEditorIdeas
            "Study" -> R.id.chipEditorStudy
            else -> R.id.chipEditorPersonal
        }
        chipGroup.check(chipId)
    }

    companion object {
        const val EXTRA_ID = "EXTRA_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CONTENT = "EXTRA_CONTENT"
        const val EXTRA_COLOR_INDEX = "EXTRA_COLOR_INDEX"
        const val EXTRA_IS_PINNED = "EXTRA_IS_PINNED"
        const val EXTRA_CATEGORY = "EXTRA_CATEGORY"
        const val EXTRA_TIMESTAMP = "EXTRA_TIMESTAMP"

        const val RESULT_DELETED = 99
    }
}
