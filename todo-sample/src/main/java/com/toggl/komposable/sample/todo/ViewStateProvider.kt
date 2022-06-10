package com.toggl.komposable.sample.todo

import kotlinx.coroutines.flow.Flow

interface ViewStateProvider<ViewState> {
    val viewState: Flow<ViewState>
    val initialViewState: ViewState
}
