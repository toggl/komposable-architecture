package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.internal.CompositeReducer
import com.toggl.komposable.internal.OptionalReducer
import com.toggl.komposable.internal.PullbackReducer

/**
 * Allows multiple reducers of the same type to be composed into a single reducer
 * This is usually used in combination with [pullback] to create the main reducer which is
 * then used to create a store
 * @see createStore
 * @see pullback
 */
fun <State, Action> combine(vararg reducers: Reducer<State, Action>): Reducer<State, Action> =
    CompositeReducer(reducers.toList())

/**
 * Allows a reducer to handle a subset of actions from another reducer
 * Use decoration to share action handling behavior across multiple reducers
 * A good use case for this is entity creation boilerplate for an entity that can be created
 * by multiple different views
 * @see com.toggl.komposable.architecture.Store.view
 */
fun <LocalState, GlobalState, LocalAction, GlobalAction> Reducer<GlobalState, GlobalAction>.decorateWith(
    reducer: Reducer<LocalState, LocalAction>,
    mapToLocalState: (GlobalState) -> LocalState,
    mapToLocalAction: (GlobalAction) -> LocalAction?,
    mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    mapToGlobalAction: (LocalAction) -> GlobalAction
): Reducer<GlobalState, GlobalAction> =
    CompositeReducer(listOf(this, reducer.pullback(mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)))

/**
 * Wraps a reducer to change its action and state type. This allows combining multiple child
 * reducers into a single parent reducer. If the child reducer relies on optional state, use
 * [optionalPullback] instead
 * @see combine
 * @see optionalPullback
 */
fun <LocalState, GlobalState, LocalAction, GlobalAction>
Reducer<LocalState, LocalAction>.pullback(
    mapToLocalState: (GlobalState) -> LocalState,
    mapToLocalAction: (GlobalAction) -> LocalAction?,
    mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    mapToGlobalAction: (LocalAction) -> GlobalAction
): Reducer<GlobalState, GlobalAction> =
    PullbackReducer(this, mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)

/**
 * Wraps a reducer to change its action and state type. This allows combining multiple child
 * reducers into a single parent reducer. If the child reducer does not rely on optional state,
 * use [pullback] instead
 * @see combine
 * @see pullback
 */
fun <LocalState, GlobalState, LocalAction, GlobalAction>
Reducer<LocalState, LocalAction>.optionalPullback(
    mapToLocalState: (GlobalState) -> LocalState?,
    mapToLocalAction: (GlobalAction) -> LocalAction?,
    mapToGlobalState: (GlobalState, LocalState?) -> GlobalState,
    mapToGlobalAction: (LocalAction) -> GlobalAction
): Reducer<GlobalState, GlobalAction> =
    OptionalReducer(this, mapToLocalState, mapToLocalAction, mapToGlobalState, mapToGlobalAction)
