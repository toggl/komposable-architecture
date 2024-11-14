package com.toggl.komposable.sample.todos.todo

import java.util.UUID

data class TodoState(
    val id: UUID,
    val description: String,
    val isComplete: Boolean,
)
