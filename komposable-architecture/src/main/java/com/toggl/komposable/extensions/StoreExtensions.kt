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

fun <State, Action : Any> Store<State, Action>.dispatch(action: Action) =
    dispatch(listOf(action))