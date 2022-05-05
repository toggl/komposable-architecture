package com.toggl.komposable.sample.todo.edit

import androidx.lifecycle.ViewModel
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.sample.todo.ViewStateProvider
import com.toggl.komposable.sample.todo.data.EditableTodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class EditStoreViewModel @Inject constructor(
    store: Store<EditState, EditAction>
) : ViewModel(), ViewStateProvider<EditViewState>, Store<EditState, EditAction> by store {
    override val viewState: Flow<EditViewState> = state.map { EditViewState(it.editableTodo) }
    override val initialViewState: EditViewState = EditViewState()
}

data class EditViewState(
    val editableTodo: EditableTodoItem = EditableTodoItem(),
)
