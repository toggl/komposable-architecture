package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.architecture.Subscription
import com.toggl.komposable.exceptions.ExceptionHandler
import com.toggl.komposable.exceptions.RethrowingExceptionHandler
import com.toggl.komposable.internal.MutableStateFlowStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.emptyFlow

/**
 * Creates a store that can be used for dispatching actions and listening to state
 * @param initialState      First state of
 * @param reducer           The global reducer, which should be a combination of all child reducers
 * @param subscription      A subscription for reacting to state changes and emit actions
 * @param storeScope        The scope in which the store will run. Defaults to GlobalScope
 * @param exceptionHandler  A handler for the exceptions thrown. Defaults to Rethrowing
 * @return A default store implementation backed by MutableStateFlow
 * @see com.toggl.komposable.exceptions.RethrowingExceptionHandler
 * @see kotlinx.coroutines.flow.MutableStateFlow
 * @see kotlinx.coroutines.GlobalScope
 */
fun <State, Action : Any> createStore(
    initialState: State,
    reducer: Reducer<State, Action>,
    subscription: Subscription<State, Action> = Subscription { emptyFlow() },
    storeScope: CoroutineScope = GlobalScope,
    exceptionHandler: ExceptionHandler = RethrowingExceptionHandler()
) = MutableStateFlowStore.create(
    initialState = initialState,
    reducer = reducer,
    subscription = subscription,
    storeScope = storeScope,
    exceptionHandler = exceptionHandler
)

/**
 * Convenience method to send a single action to be processed by the internal reducer
 * @see Store.dispatch
 */
fun <State, Action : Any> Store<State, Action>.dispatch(action: Action) =
    dispatch(listOf(action))