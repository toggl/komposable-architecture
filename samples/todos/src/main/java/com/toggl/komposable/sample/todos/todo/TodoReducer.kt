package com.toggl.komposable.sample.todos.todo

import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.withoutEffect

class TodoReducer : Reducer<TodoState, TodoAction> {
    override fun reduce(
        state: TodoState,
        action: TodoAction,
    ): ReduceResult<TodoState, TodoAction> =
        when (action) {
            is TodoAction.DescriptionChanged -> state.copy(description = action.description).withoutEffect()
            is TodoAction.IsCompleteChanged -> state.copy(isComplete = action.isComplete).withoutEffect()
        }
}
