package com.example.notesmanagementapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import android.widget.ImageButton
import com.example.notesmanagementapp.data.model.Note
import com.example.notesmanagementapp.ui.AddEditNoteActivity
import com.example.notesmanagementapp.ui.NoteAdapter
import com.example.notesmanagementapp.viewmodel.NoteViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val noteViewModel: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter

    private lateinit var etSearch: EditText
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvNotes: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var fabAddNote: FloatingActionButton

    private var allNotesList: List<Note> = emptyList()
    private var selectedFilterCategory: String = "All"
    private var isDarkMode: Boolean = false

    private val startAddNoteResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val title = data.getStringExtra(AddEditNoteActivity.EXTRA_TITLE) ?: ""
            val content = data.getStringExtra(AddEditNoteActivity.EXTRA_CONTENT) ?: ""
            val colorIndex = data.getIntExtra(AddEditNoteActivity.EXTRA_COLOR_INDEX, 0)
            val isPinned = data.getBooleanExtra(AddEditNoteActivity.EXTRA_IS_PINNED, false)
            val category = data.getStringExtra(AddEditNoteActivity.EXTRA_CATEGORY) ?: "Personal"

            val note = Note(
                title = title,
                content = content,
                timestamp = System.currentTimeMillis(),
                colorIndex = colorIndex,
                isPinned = isPinned,
                category = category
            )
            noteViewModel.insert(note)
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
        }
    }

    private val startEditNoteResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@registerForActivityResult
        val id = data.getIntExtra(AddEditNoteActivity.EXTRA_ID, -1)
        if (id == -1) return@registerForActivityResult

        if (result.resultCode == RESULT_OK) {
            val title = data.getStringExtra(AddEditNoteActivity.EXTRA_TITLE) ?: ""
            val content = data.getStringExtra(AddEditNoteActivity.EXTRA_CONTENT) ?: ""
            val colorIndex = data.getIntExtra(AddEditNoteActivity.EXTRA_COLOR_INDEX, 0)
            val isPinned = data.getBooleanExtra(AddEditNoteActivity.EXTRA_IS_PINNED, false)
            val category = data.getStringExtra(AddEditNoteActivity.EXTRA_CATEGORY) ?: "Personal"

            val note = Note(
                id = id,
                title = title,
                content = content,
                timestamp = System.currentTimeMillis(),
                colorIndex = colorIndex,
                isPinned = isPinned,
                category = category
            )
            noteViewModel.update(note)
            Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show()
        } else if (result.resultCode == AddEditNoteActivity.RESULT_DELETED) {
            val title = data.getStringExtra(AddEditNoteActivity.EXTRA_TITLE) ?: ""
            val content = data.getStringExtra(AddEditNoteActivity.EXTRA_CONTENT) ?: ""
            val colorIndex = data.getIntExtra(AddEditNoteActivity.EXTRA_COLOR_INDEX, 0)
            val isPinned = data.getBooleanExtra(AddEditNoteActivity.EXTRA_IS_PINNED, false)
            val category = data.getStringExtra(AddEditNoteActivity.EXTRA_CATEGORY) ?: "Personal"

            val noteToDelete = Note(
                id = id,
                title = title,
                content = content,
                timestamp = System.currentTimeMillis(),
                colorIndex = colorIndex,
                isPinned = isPinned,
                category = category
            )
            noteViewModel.delete(noteToDelete)
            showUndoSnackbar(noteToDelete)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = getSharedPreferences("ThemePrefs", MODE_PRIVATE)
        isDarkMode = sharedPreferences.getBoolean("isDarkMode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Initialize Views
        etSearch = findViewById(R.id.etSearch)
        chipGroup = findViewById(R.id.chipGroupCategories)
        rvNotes = findViewById(R.id.rvNotes)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        fabAddNote = findViewById(R.id.fabAddNote)

        val btnThemeToggle = findViewById<ImageButton>(R.id.btnThemeToggle)
        if (isDarkMode) {
            btnThemeToggle.setImageResource(R.drawable.ic_sun)
        } else {
            btnThemeToggle.setImageResource(R.drawable.ic_moon)
        }

        btnThemeToggle.setOnClickListener {
            isDarkMode = !isDarkMode
            sharedPreferences.edit().putBoolean("isDarkMode", isDarkMode).apply()
            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Setup RecyclerView
        adapter = NoteAdapter { note ->
            val intent = Intent(this, AddEditNoteActivity::class.java).apply {
                putExtra(AddEditNoteActivity.EXTRA_ID, note.id)
                putExtra(AddEditNoteActivity.EXTRA_TITLE, note.title)
                putExtra(AddEditNoteActivity.EXTRA_CONTENT, note.content)
                putExtra(AddEditNoteActivity.EXTRA_COLOR_INDEX, note.colorIndex)
                putExtra(AddEditNoteActivity.EXTRA_IS_PINNED, note.isPinned)
                putExtra(AddEditNoteActivity.EXTRA_CATEGORY, note.category)
                putExtra(AddEditNoteActivity.EXTRA_TIMESTAMP, note.timestamp)
            }
            startEditNoteResult.launch(intent)
        }

        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        rvNotes.layoutManager = layoutManager
        rvNotes.adapter = adapter

        // Observe Notes
        noteViewModel.allNotes.observe(this) { notes ->
            allNotesList = notes
            updateNotesList()
        }

        // Setup search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateNotesList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup categories filter
        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedFilterCategory = if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                chip.text.toString()
            } else {
                "All"
            }
            updateNotesList()
        }

        // FAB Click
        fabAddNote.setOnClickListener {
            val intent = Intent(this, AddEditNoteActivity::class.java)
            startAddNoteResult.launch(intent)
        }

        // Scroll listener to animate FAB
        rvNotes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && fabAddNote.isShown) {
                    fabAddNote.hide()
                } else if (dy < 0 && !fabAddNote.isShown) {
                    fabAddNote.show()
                }
            }
        })

        // Setup Swipe to Delete
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val noteToDelete = adapter.currentList[position]
                noteViewModel.delete(noteToDelete)
                showUndoSnackbar(noteToDelete)
            }
        })
        itemTouchHelper.attachToRecyclerView(rvNotes)
    }

    private fun updateNotesList() {
        val searchQuery = etSearch.text.toString().trim().lowercase(Locale.getDefault())

        val filteredList = allNotesList.filter { note ->
            val matchesCategory = (selectedFilterCategory == "All") || (note.category == selectedFilterCategory)
            val matchesSearch = note.title.lowercase(Locale.getDefault()).contains(searchQuery) ||
                    note.content.lowercase(Locale.getDefault()).contains(searchQuery)
            matchesCategory && matchesSearch
        }

        adapter.submitList(filteredList) {
            layoutEmptyState.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showUndoSnackbar(note: Note) {
        Snackbar.make(findViewById(R.id.main), "Note deleted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                noteViewModel.insert(note)
            }
            .show()
    }
}