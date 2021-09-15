package com.toggl.komposable.sample.todo.list

import com.toggl.komposable.sample.todo.BackStack
import com.toggl.komposable.sample.todo.BackStackAwareState
import com.toggl.komposable.sample.todo.data.TodoItem

data class ListState(
    val todoList: List<TodoItem>,
    override val backStack: BackStack,
) : BackStackAwareState<ListState> {
    override fun changeBackStack(backStack: BackStack): ListState =
        copy(backStack = backStack)
}
