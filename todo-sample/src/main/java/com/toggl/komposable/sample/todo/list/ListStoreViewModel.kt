package com.toggl.komposable.sample.todo.list

import androidx.lifecycle.ViewModel
import com.toggl.komposable.architecture.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ListStoreViewModel @Inject constructor(
    store: Store<ListState, ListAction>
) : ViewModel(), Store<ListState, ListAction> by store
