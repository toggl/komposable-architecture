package com.toggl.komposable.sample.todos.title

import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.withoutEffect

class TitleReducer : Reducer<TitleState, TitleAction> {
    override fun reduce(
        state: TitleState,
        action: TitleAction,
    ): ReduceResult<TitleState, TitleAction> =
        when (action) {
            is TitleAction.TitleChanged -> state.copy(title = action.text).withoutEffect()
            TitleAction.StartEditing -> state.copy(isEdited = true).withoutEffect()
            TitleAction.StopEditing -> state.copy(isEdited = false).withoutEffect()
        }
}
