package com.toggl.komposable.sample.todo.list

import com.toggl.komposable.sample.todo.TodoItem

sealed class ListAction {
    data class ListUpdated(val todoList: List<TodoItem>) : ListAction()
    data class TodoTapped(val todo: TodoItem) : ListAction()
    object AddTodoTapped : ListAction()
}
