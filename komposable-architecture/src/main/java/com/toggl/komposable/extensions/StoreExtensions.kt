package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Subscription
import com.toggl.komposable.exceptions.ExceptionHandler
import com.toggl.komposable.exceptions.RethrowingExceptionHandler
import com.toggl.komposable.internal.MutableStateFlowStore
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.scope.StoreScopeProvider
import kotlinx.coroutines.flow.emptyFlow

/**
 * Creates a store that can be used for sending actions and listening to state
 * @param initialState      First state of
 * @param reducer           The global reducer, which should be a combination of all child reducers
 * @param subscription      A subscription for reacting to state changes and emit actions
 * @param exceptionHandler  A handler for the exceptions thrown. Defaults to Rethrowing
 * @param storeScopeProvider        Provider of the scope in which the store will run
 * @param dispatcherProvider        Provider of CoroutineDispatchers to be used inside of the store
 * @return A default store implementation backed by MutableStateFlow
 * @see com.toggl.komposable.exceptions.RethrowingExceptionHandler
 * @see kotlinx.coroutines.flow.MutableStateFlow
 * @see kotlinx.coroutines.GlobalScope
 */
fun <State, Action : Any> createStore(
    initialState: State,
    reducer: Reducer<State, Action>,
    subscription: Subscription<State, Action> = Subscription { emptyFlow() },
    exceptionHandler: ExceptionHandler = RethrowingExceptionHandler(),
    storeScopeProvider: StoreScopeProvider,
    dispatcherProvider: DispatcherProvider
) = MutableStateFlowStore.create(
    initialState = initialState,
    reducer = reducer,
    subscription = subscription,
    exceptionHandler = exceptionHandler,
    storeScopeProvider = storeScopeProvider,
    dispatcherProvider = dispatcherProvider
)
