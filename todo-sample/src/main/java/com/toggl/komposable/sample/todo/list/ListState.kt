package com.toggl.komposable.sample.todo.list

import com.toggl.komposable.sample.todo.BackStack
import com.toggl.komposable.sample.todo.TodoItem

data class ListState(
    val todoList: List<TodoItem>,
    val backStack: BackStack,
)
