package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.internal.CompositeReducer
import com.toggl.komposable.internal.OptionalReducer
import com.toggl.komposable.internal.PullbackReducer

fun <State, Action> combine(vararg reducers: Reducer<State, Action>): Reducer<State, Action> =
    CompositeReducer(reducers.toList())

fun <LocalState, GlobalState, LocalAction, GlobalAction> Reducer<GlobalState, GlobalAction>.decorateWith(
    reducer: Reducer<LocalState, LocalAction>,
    mapToLocalState: (GlobalState) -> LocalState,
    mapToLocalAction: (GlobalAction) -> LocalAction?,
    mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    mapToGlobalAction: (LocalAction) -> GlobalAction
): Reducer<GlobalState, GlobalAction> =
    CompositeReducer(listOf(this, reducer.pullback(mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)))

fun <LocalState, GlobalState, LocalAction, GlobalAction>
    Reducer<LocalState, LocalAction>.pullback(
    mapToLocalState: (GlobalState) -> LocalState,
    mapToLocalAction: (GlobalAction) -> LocalAction?,
    mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    mapToGlobalAction: (LocalAction) -> GlobalAction
): Reducer<GlobalState, GlobalAction> =
    PullbackReducer(this, mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)

fun <LocalState, GlobalState, LocalAction, GlobalAction>
    Reducer<LocalState, LocalAction>.optionalPullback(
    mapToLocalState: (GlobalState) -> LocalState?,
    mapToLocalAction: (GlobalAction) -> LocalAction?,
    mapToGlobalState: (GlobalState, LocalState?) -> GlobalState,
    mapToGlobalAction: (LocalAction) -> GlobalAction
): Reducer<GlobalState, GlobalAction> =
    OptionalReducer(this, mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)
