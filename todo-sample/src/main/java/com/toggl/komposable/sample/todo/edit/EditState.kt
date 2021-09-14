package com.toggl.komposable.sample.todo.edit

import com.toggl.komposable.sample.todo.BackStack
import com.toggl.komposable.sample.todo.BackStackAwareState
import com.toggl.komposable.sample.todo.data.EditableTodoItem

data class EditState(
    val editableTodo: EditableTodoItem = EditableTodoItem(title = "", description = ""),
    override val backStack: BackStack
) : BackStackAwareState<EditState> {
    override fun changeBackStack(backStack: BackStack): EditState =
        copy(backStack = backStack)
}
