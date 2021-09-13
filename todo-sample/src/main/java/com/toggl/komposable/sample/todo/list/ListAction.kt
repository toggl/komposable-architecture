package com.toggl.komposable.sample.todo.list

import com.toggl.komposable.sample.todo.data.TodoItem

sealed class ListAction {
    data class ListUpdated(val todoList: List<TodoItem>) : ListAction()
    object AddTodoTapped : ListAction()
}
