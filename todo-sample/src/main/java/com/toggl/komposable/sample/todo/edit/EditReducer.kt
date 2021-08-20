package com.toggl.komposable.sample.todo.edit

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.mutateWithoutEffects
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditReducer @Inject constructor() : Reducer<EditState, EditAction> {
    override fun reduce(state: Mutable<EditState>, action: EditAction): List<Effect<EditAction>> =
        when (action) {
            is EditAction.DescriptionChanged -> state.mutateWithoutEffects { copy(editableTodo = editableTodo.copy(description = action.description)) }
            is EditAction.TitleChanged -> state.mutateWithoutEffects { copy(editableTodo = editableTodo.copy(title = action.title)) }
            EditAction.CloseTapped -> TODO()
            EditAction.SaveTapped -> TODO("Return save effect which would result in close action")
        }
}
