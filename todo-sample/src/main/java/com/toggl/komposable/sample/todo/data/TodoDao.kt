package com.toggl.komposable.sample.todo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo")
    fun getAll(): Flow<List<TodoItem>>

    @Update
    fun update(user: TodoItem)

    @Insert
    fun insert(user: TodoItem)

    @Delete
    fun delete(user: TodoItem)
}