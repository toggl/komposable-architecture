package com.toggl.komposable.sample.todo.data

data class Todo(val id: Long, val title: String, val description: String)

data class EditableTodo(val title: String, val description: String)
