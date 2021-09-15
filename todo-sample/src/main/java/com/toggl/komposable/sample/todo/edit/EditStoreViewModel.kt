package com.toggl.komposable.sample.todo.edit

import androidx.lifecycle.ViewModel
import com.toggl.komposable.architecture.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditStoreViewModel @Inject constructor(
    store: Store<EditState, EditAction>
) : ViewModel(), Store<EditState, EditAction> by store
