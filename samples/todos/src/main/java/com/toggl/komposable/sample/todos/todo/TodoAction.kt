package com.toggl.komposable.sample.todos.todo

sealed class TodoAction {
    data class IsCompleteChanged(
        val isComplete: Boolean,
    ) : TodoAction()

    data class DescriptionChanged(
        val description: String,
    ) : TodoAction()
}
