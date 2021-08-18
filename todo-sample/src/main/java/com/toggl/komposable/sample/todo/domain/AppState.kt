package com.toggl.komposable.sample.todo.domain

import com.toggl.komposable.sample.todo.data.EditableTodo
import com.toggl.komposable.sample.todo.data.Todo

data class AppState(
    val todoList: List<Todo> = emptyList(),
    val editableTodo: EditableTodo = EditableTodo("", "")
)
