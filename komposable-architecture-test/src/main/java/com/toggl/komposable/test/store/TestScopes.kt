package com.toggl.komposable.test.store

import kotlinx.coroutines.test.advanceTimeBy
import kotlin.time.Duration

interface ExhaustiveTestStoreScope<State : Any, Action : Any?> {

    /**
     * The current state of the store.
     */
    val state: State

    /**
     * Sends an action to the store and asserts the resulting state change.
     * Not providing an assert function will assert that the state doesn't change.
     *
     * @param action The action to be sent to the store.
     * @param assert A lambda function for asserting the resulting state.
     */
    suspend fun send(action: Action, assert: ((state: State) -> State)? = null)

    /**
     * Receives an action that has been fired by some Effect running in the store and
     * asserts the resulting state change.
     * Don't provide an assert function if you don't expect a state change.
     *
     * @param action The action expected to be received.
     * @param assert A lambda function for asserting the resulting state.
     */
    suspend fun receive(action: Action, assert: ((state: State) -> State)? = null)

    /**
     * Receives an action that has been fired by some Effect running in the store
     * based on a predicate and asserts the resulting state change.
     * Don't provide an assert function if you don't expect a state change.
     *
     * @param actionPredicate A predicate function for selecting the received action.
     * @param assert A lambda function for asserting the resulting state.
     */
    suspend fun receive(
        actionPredicate: (Action) -> Boolean,
        assert: ((state: State) -> State)? = null,
    )

    /**
     * Advances the time in the associated TestStore's coroutine scope.
     *
     * @param duration The duration by which to advance the time.
     */
    fun advanceTestStoreTimeBy(duration: Duration)
}

interface NonExhaustiveTestStoreScope<State : Any, Action : Any?> :
    ExhaustiveTestStoreScope<State, Action> {

    val defaultDuration: Duration

    /**
     * Skips a specified number of received actions without asserting state changes.
     * Note that skipped received actions still incur in state changes.
     *
     * @param count The number of received actions to skip.
     */
    suspend fun skipReceivedActions(count: Int)

    /**
     * Wait for all effects to finish under the real world [timeout] duration.
     * Don't use [timeout] to wait for any virtual time your effects might be taking to complete, use [advanceTestStoreTimeBy] instead.
     * All ignored received actions will be skipped and their state changes integrated into the final state of the store.
     * The final state of the store can be asserted using [assert].
     */
    suspend fun awaitEffectsConsumption(timeout: Duration = defaultDuration)

    /**
     * Asserts the current state of the store.
     */
    fun assert(assert: (state: State) -> State)
}

internal open class BaseTestStoreScopeImpl<State : Any, Action : Any?>(private val store: TestStore<State, Action>) : ExhaustiveTestStoreScope<State, Action> {
    override val state: State
        get() = store.state

    override suspend fun send(action: Action, assert: ((state: State) -> State)?) =
        store.send(action, assert)

    override suspend fun receive(action: Action, assert: ((state: State) -> State)?) =
        store.receive(action, assert)

    override suspend fun receive(
        actionPredicate: (Action) -> Boolean,
        assert: ((state: State) -> State)?,
    ) = store.receive(actionPredicate, assert)

    override fun advanceTestStoreTimeBy(duration: Duration) {
        store.testCoroutineScope.advanceTimeBy(duration)
    }
}

internal class ExhaustiveTestStoreScopeImpl<State : Any, Action : Any?>(store: TestStore<State, Action>) :
    ExhaustiveTestStoreScope<State, Action>, BaseTestStoreScopeImpl<State, Action>(store)

internal class NonExhaustiveTestStoreScopeImpl<State : Any, Action : Any?>(private val store: TestStore<State, Action>) :
    NonExhaustiveTestStoreScope<State, Action>, BaseTestStoreScopeImpl<State, Action>(store) {

    override val defaultDuration: Duration
        get() = store.timeout

    override suspend fun skipReceivedActions(count: Int) {
        store.skipReceivedActions(count)
    }

    override suspend fun awaitEffectsConsumption(timeout: Duration) {
        val previousTimeout = store.timeout
        store.timeout = timeout
        store.finish()
        store.timeout = previousTimeout
    }

    override fun assert(assert: (state: State) -> State) {
        store.assert(assert)
    }
}
