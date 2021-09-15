package com.toggl.komposable.sample.todo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo")
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val title: String,
    val description: String
)

data class EditableTodoItem(
    val id: Long? = null,
    val title: String = "",
    val description: String = ""
)
