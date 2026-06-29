package com.example.notesmanagementapp.data.repository

import com.example.notesmanagementapp.data.local.NoteDao
import com.example.notesmanagementapp.data.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes("%$query%")
    }

    suspend fun insert(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun delete(note: Note) {
        noteDao.deleteNote(note)
    }
}
