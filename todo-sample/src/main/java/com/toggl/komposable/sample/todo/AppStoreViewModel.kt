package com.toggl.komposable.sample.todo

import androidx.lifecycle.ViewModel
import com.toggl.komposable.architecture.Store
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppStoreViewModel @Inject constructor(
    store: Store<AppState, AppAction>
) : ViewModel(), Store<AppState, AppAction> by store
