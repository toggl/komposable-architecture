package com.toggl.komposable.architecture

import kotlinx.coroutines.flow.Flow

interface Store<State, Action : Any> {
    val state: Flow<State>
    fun dispatch(actions: List<Action>)

    fun <ViewState, ViewAction : Any> view(
        mapToLocalState: (State) -> ViewState,
        mapToGlobalAction: (ViewAction) -> Action?
    ): Store<ViewState, ViewAction>

    fun <ViewState : Any, ViewAction : Any> optionalView(
        mapToLocalState: (State) -> ViewState?,
        mapToGlobalAction: (ViewAction) -> Action?
    ): Store<ViewState, ViewAction>
}