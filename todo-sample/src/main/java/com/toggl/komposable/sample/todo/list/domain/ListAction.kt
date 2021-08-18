package com.toggl.komposable.sample.todo.list.domain

import com.toggl.komposable.sample.todo.data.Todo

sealed class ListAction {
    data class ListUpdated(val todoList: List<Todo>) : ListAction()
    data class TodoTapped(val todo: Todo) : ListAction()
    object AddTodoTapped : ListAction()
}
