package com.toggl.komposable.test.store

import com.toggl.komposable.architecture.Reducer

/**
 * Point of entry to test reducers exhaustively.
 * Automatically finishes the test after the test body has been executed, asserting that all effects
 * are done all actions have been received.
 *
 * @param initialState The initial state of the store.
 * @param config The configuration for the test.
 * @param testBody The test body to be executed.
 */
suspend fun <State : Any, Action : Any?> Reducer<State, Action>.test(
    initialState: State,
    config: ExhaustiveTestConfig,
    testBody: suspend ExhaustiveTestStoreScope<State, Action>.() -> Unit,
) {
    val testStore = TestStore(
        initialState = { initialState },
        reducer = { this },
        config = config,
    )
    val scope = ExhaustiveTestStoreScopeImpl(testStore)
    testBody(scope)
    testStore.finish()
}

/**
 * Point of entry to test reducers non exhaustively.
 * Does NOT automatically finish the test after the test body has been executed. That is, it doesn't
 * assert or ensure that all effects are done and all actions have been received.
 * Any number of actions can be sent and received during the test body.
 * Actions fired by effects still must be received or skipped before new actions can be sent.
 */
suspend fun <State : Any, Action : Any?> Reducer<State, Action>.test(
    initialState: State,
    config: NonExhaustiveTestConfig,
    testBody: suspend NonExhaustiveTestStoreScope<State, Action>.() -> Unit,
) {
    val testStore = TestStore(
        initialState = { initialState },
        reducer = { this },
        config = config,
    )
    val scope = NonExhaustiveTestStoreScopeImpl(testStore)
    testBody(scope)
}
