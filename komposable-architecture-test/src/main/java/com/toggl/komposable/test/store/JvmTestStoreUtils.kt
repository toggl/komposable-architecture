package com.toggl.komposable.test.store

import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.scope.DispatcherProvider
import kotlinx.coroutines.test.TestScope
import java.util.logging.Logger

fun <State : Any, Action> createTestStore(
    initialState: () -> State,
    reducer: () -> Reducer<State, Action>,
    dispatcherProvider: DispatcherProvider,
    testCoroutineScope: TestScope,
): TestStore<State, Action> = TestStore(
    initialState = initialState,
    reducer = reducer,
    dispatcherProvider = dispatcherProvider,
    logger = JavaLogger(Logger.getLogger("TestStore")),
    reflectionHandler = JvmReflectionHandler(),
    testCoroutineScope = testCoroutineScope,
)
