package com.example.taskmanagementapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.taskmanagementapp.data.local.TaskDao
import com.example.taskmanagementapp.data.local.TaskDatabase
import com.example.taskmanagementapp.data.model.Task
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TaskDatabaseTest {

    private lateinit var db: TaskDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java).build()
        taskDao = db.taskDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeTaskAndReadInList() = runBlocking {
        val task = Task(
            title = "Test Task",
            description = "Test Description",
            dueDate = System.currentTimeMillis(),
            priority = "HIGH",
            status = "PENDING",
            category = "WORK"
        )
        taskDao.insertTask(task)
        val allTasks = taskDao.getAllTasks().first()
        assertEquals(allTasks.size, 1)
        assertEquals(allTasks[0].title, "Test Task")
        assertEquals(allTasks[0].description, "Test Description")
        assertEquals(allTasks[0].priority, "HIGH")
        assertEquals(allTasks[0].category, "WORK")
    }

    @Test
    @Throws(Exception::class)
    fun updateTaskAndRead() = runBlocking {
        val task = Task(
            id = 1,
            title = "Test Task",
            description = "Test Description",
            dueDate = System.currentTimeMillis(),
            priority = "HIGH",
            status = "PENDING",
            category = "WORK"
        )
        taskDao.insertTask(task)

        val savedTask = taskDao.getTaskById(1)
        assertNotNull(savedTask)

        val updatedTask = savedTask!!.copy(status = "COMPLETED")
        taskDao.updateTask(updatedTask)

        val retrievedTask = taskDao.getTaskById(1)
        assertEquals(retrievedTask?.status, "COMPLETED")
    }
}
