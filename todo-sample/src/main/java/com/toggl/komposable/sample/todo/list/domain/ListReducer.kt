package com.toggl.komposable.sample.todo.list.domain

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.mutateWithoutEffects

class ListReducer : Reducer<ListState, ListAction> {
    override fun reduce(state: Mutable<ListState>, action: ListAction): List<Effect<ListAction>> =
        when (action) {
            is ListAction.ListUpdated -> state.mutateWithoutEffects { copy(todoList = action.todoList) }
            ListAction.AddTodoTapped -> TODO()
            is ListAction.TodoTapped -> TODO()
        }
}
