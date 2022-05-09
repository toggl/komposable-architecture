package com.toggl.komposable.sample.todo.list

import androidx.lifecycle.ViewModel
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.sample.todo.ViewStateProvider
import com.toggl.komposable.sample.todo.data.TodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class ListStoreViewModel @Inject constructor(
    store: Store<ListState, ListAction>
) : ViewModel(), ViewStateProvider<ListViewState>, Store<ListState, ListAction> by store {
    override val viewState: Flow<ListViewState> = state.map { ListViewState(it.todoList) }
    override val initialViewState: ListViewState = ListViewState()
}

data class ListViewState(
    val todoList: List<TodoItem> = emptyList(),
)
