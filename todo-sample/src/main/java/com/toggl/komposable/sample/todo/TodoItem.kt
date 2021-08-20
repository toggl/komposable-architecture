package com.toggl.komposable.sample.todo

data class TodoItem(val id: Long, val title: String, val description: String)

data class EditableTodoItem(val title: String, val description: String)
