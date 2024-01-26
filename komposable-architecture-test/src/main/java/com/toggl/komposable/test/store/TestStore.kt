package com.toggl.komposable.test.store

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.extensions.createStore
import com.toggl.komposable.extensions.map
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.test.utils.Logger
import com.toggl.komposable.test.utils.NoopLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.yield
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * A testing utility for managing and testing state changes in a Redux-like architecture.
 *
 * @param initialState A lambda function providing the initial state of the store.
 * @param reducer A lambda function providing the reducer that will power the store.
 * @param dispatcherProvider Provides the dispatcher for executing actions.
 * @param logger A logger for handling log messages during testing.
 * @param reflectionHandler Handles reflection for asserting state changes.
 * @param testCoroutineScope The coroutine scope for running tests.
 * @param timeout Timeout duration for waiting effects to complete.
 * @param exhaustivity The level of exhaustiveness for testing.
 */
class TestStore<State : Any, Action : Any?> internal constructor(
    initialState: () -> State,
    reducer: () -> Reducer<State, Action>,
    config: TestConfig,
) {
    internal val testCoroutineScope: TestScope = config.testCoroutineScope
    internal val logger: Logger = config.logger
    internal val reflectionHandler: ReflectionHandler = config.reflectionHandler
    internal var timeout: Duration = config.timeout
    private val exhaustivity: Exhaustivity = config.exhaustivity
    private var tempExhaustivity: Exhaustivity? = null
    internal val reducer: TestReducer<State, Action>
    private val store: Store<State, TestReducer.TestAction<Action>>
    val state: State
        get() = reducer.state

    private val effectSubscriptionChannel = Channel<Unit>(capacity = BUFFERED)

    init {
        this.reducer = TestReducer(
            reducer(),
            initialState(),
            testCoroutineScope,
            effectSubscriptionChannel,
        )
        this.store = createStore(
            initialState = this.state,
            reducer = this.reducer,
            dispatcherProvider = config.dispatcherProvider,
            storeScopeProvider = { testCoroutineScope },
        )
    }

    private val assertionRunner: AssertionRunner<State, Action>
        get() = (tempExhaustivity ?: exhaustivity).createAssertionRunner(this)


    private suspend fun withTempExhaustivity(exhaustivity: Exhaustivity, block: suspend () -> Unit) {
        tempExhaustivity = exhaustivity
        block()
        tempExhaustivity = null
    }

    internal interface AssertionRunner<State : Any, Action> {
        fun assertStateChange(
            previousState: State,
            currentState: State,
            assert: ((state: State) -> State)?,
        )

        fun hasReceivedActionToHandle(match: (Action) -> Boolean): Boolean
        fun skipNotMatchingActions(match: (Action) -> Boolean)
        fun assertEffectsAreDone()
        suspend fun assertActionsWereReceived()
    }

    /**
     * Controls the level of exhaustiveness for state change and action receipt assertions in [TestStore].
     */
    sealed class Exhaustivity {
        /**
         * Represents an exhaustive level where all state changes and received actions must be asserted and
         * all effects must be done when finishing the test.
         */
        data object Exhaustive : Exhaustivity()

        /**
         * Represents a non-exhaustive level where state changes and received actions can be skipped.
         * Note that actions fired by effects must still be received or skipped before new actions can be sent.
         * The final state of the store can be asserted using [TestStore.assert] after calling [TestStore.finish]
         */
        data class NonExhaustive(
            val logIgnoredReceivedActions: Boolean = true,
            val logIgnoredStateChanges: Boolean = true,
            val logIgnoredEffects: Boolean = true,
        ) : Exhaustivity()
    }

    /**
     * Sends an action to the store and asserts the resulting state change.
     * Don't provide an assert function if you don't expect a state change.
     *
     * @param action The action to be sent to the store.
     * @param assert A lambda function for asserting the resulting state.
     */
    internal suspend fun send(action: Action, assert: ((state: State) -> State)? = null) {
        testCoroutineScope.runCurrent()
        if (reducer.receivedActions.isNotEmpty()) {
            throw AssertionError("Cannot send actions after receiving actions")
        }

        val previousState = reducer.state
        store.send(TestReducer.TestAction(TestReducer.TestAction.Origin.Send(action)))
        effectSubscriptionChannel.receiveCatching()
        testCoroutineScope.runCurrent()
        val currentState = reducer.state

        assertionRunner.assertStateChange(previousState, currentState, assert)
    }

    /**
     * Receives an action that has been fired by some Effect running in the store and
     * asserts the resulting state change.
     * Don't provide an assert function if you don't expect a state change.
     *
     * @param action The action expected to be received.
     * @param assert A lambda function for asserting the resulting state.
     */
    internal suspend fun receive(action: Action, assert: ((state: State) -> State)? = null) {
        receive({ it == action }, assert)
    }

    /**
     * Receives an action that has been fired by some Effect running in the store
     * based on a predicate and asserts the resulting state change.
     * Don't provide an assert function if you don't expect a state change.
     *
     * @param actionPredicate A predicate function for selecting the received action.
     * @param assert A lambda function for asserting the resulting state.
     */
    internal suspend fun receive(
        actionPredicate: (Action) -> Boolean,
        assert: ((state: State) -> State)? = null,
    ) {
        if (reducer.inFlightEffects.isNotEmpty()) {
            awaitActionFromEffects(timeout, actionPredicate)
        }
        receiveAction({ actionPredicate(it) }, assert)
    }

    private fun receiveAction(match: (Action) -> Boolean, assert: ((state: State) -> State)?) {
        testCoroutineScope.runCurrent()
        if (reducer.receivedActions.isEmpty()) {
            throw AssertionError("No action has been received")
        }

        assertionRunner.skipNotMatchingActions(match)

        val (action, reducedState) = reducer.receivedActions.removeFirst()
        if (!match(action)) {
            throw AssertionError("Next action($action) in the queue doesn't match the predicate")
        }

        val currentState = reducer.state
        reducer.state = reducedState

        assertionRunner.assertStateChange(currentState, reducedState, assert)
        testCoroutineScope.runCurrent()
    }

    private suspend fun awaitMatch(
        timeout: Duration,
        match: () -> Boolean,
        onTimeout: () -> Unit,
    ) {
        testCoroutineScope.runCurrent()
        val start = TimeSource.Monotonic.markNow()
        while (currentCoroutineContext().isActive) {
            testCoroutineScope.runCurrent()
            yield()
            if (match()) {
                return
            }
            if (start.elapsedNow() > timeout) {
                onTimeout()
                return
            }
        }
    }

    private suspend fun awaitActionFromEffects(
        timeout: Duration,
        match: (Action) -> Boolean,
    ) = awaitMatch(
        timeout = timeout,
        match = { assertionRunner.hasReceivedActionToHandle(match) },
        onTimeout = {
            if (reducer.inFlightEffects.isEmpty()) {
                throw AssertionError("No effect in flight was found that could deliver the action, maybe it has been cancelled?")
            } else {
                throw AssertionError(
                    """
                There are in-flight effects that could deliver the action but they haven't finished under the ${timeout.inWholeMilliseconds}ms timeout.
                Try moving the scheduler forward with `testCoroutineScope.advanceTimeBy(timeoutInMillis)` or `testCoroutineScope.advanceUntilIdle()`
                before trying to receive the action.
                # In-flight effects: ${reducer.inFlightEffects}
                    """.trimIndent(),
                )
            }
        },
    )

    /**
     * Skips a specified number of received actions without asserting state changes.
     * Note that skipped received actions still incur in state changes.
     *
     * @param count The number of received actions to skip.
     */
    internal suspend fun skipReceivedActions(count: Int) {
        withTempExhaustivity(Exhaustivity.NonExhaustive(
            logIgnoredReceivedActions = false,
            logIgnoredStateChanges = false,
            logIgnoredEffects = false,
        )) {
            repeat(count) {
                receive({ true }, null)
            }
        }
    }

    /**
     * Finishes the testing process by ensuring all effects are done and all fired actions have been
     * received and their state changes asserted.
     *
     * When running tests with [Exhaustivity.NonExhaustive], this method may be called to wait for all effects
     * to finish under the [TestStore.timeout] duration. All ignored received actions will be skipped and their state changes
     * integrated into the final state of the store. The final state of the store can be asserted using [TestStore.assert].
     */
    internal suspend fun finish() {
        if (reducer.inFlightEffects.isNotEmpty()) {
            awaitMatch(
                timeout = timeout,
                match = { reducer.inFlightEffects.isEmpty() },
                onTimeout = { },
            )
        }
        assertionRunner.assertEffectsAreDone()
        assertionRunner.assertActionsWereReceived()
    }

    /**
     * Asserts the final state of the store. Only useful when running tests with [Exhaustivity.NonExhaustive].
     * See [TestStore.finish].
     */
    internal fun assert(assert: (state: State) -> State) {
        assertionRunner.assertStateChange(reducer.state, reducer.state, assert)
    }

    internal class TestReducer<State, Action>(
        private val innerReducer: Reducer<State, Action>,
        internal var state: State,
        private val testCoroutineScope: TestScope,
        private val subChannel: Channel<Unit>,
    ) : Reducer<State, TestReducer.TestAction<Action>> {
        val receivedActions: MutableList<Pair<Action, State>> = mutableListOf()
        val inFlightEffects: MutableList<LongLivingEffect<Action>> = mutableListOf()

        data class TestAction<Action>(val origin: Origin<Action>) {
            val action: Action
                get() = origin.action

            sealed class Origin<Action> {
                abstract val action: Action

                data class Send<Action>(override val action: Action) : Origin<Action>()
                data class Receive<Action>(override val action: Action) : Origin<Action>()
            }
        }

        data class LongLivingEffect<Action>(
            val id: Long = Random.nextLong(),
            val action: TestAction<Action>,
        )

        override fun reduce(
            state: State,
            action: TestAction<Action>,
        ): ReduceResult<State, TestAction<Action>> {
            val updatedState: State
            val effect: Effect<Action>
            when (action.origin) {
                is TestAction.Origin.Send -> {
                    val (newState, sentEffect) = innerReducer.reduce(state, action.action)
                    effect = sentEffect
                    updatedState = newState
                    this.state = newState
                }

                is TestAction.Origin.Receive -> {
                    val (newState, receivedEffect) = innerReducer.reduce(state, action.action)
                    effect = receivedEffect
                    updatedState = newState
                    receivedActions.add(action.action to newState)
                }
            }

            val mappedEffect = if (effect == Effect.none()) {
                testCoroutineScope.launch { subChannel.send(Unit) }
                Effect.none()
            } else {
                val longLivingEffect = LongLivingEffect(action = action)
                Effect.fromFlow(
                    effect.run().onStart {
                        subChannel.send(Unit)
                        inFlightEffects.add(longLivingEffect)
                    }.onCompletion {
                        // Completion might mean completion or cancellation
                        inFlightEffects.remove(longLivingEffect)
                    },
                )
            }

            return ReduceResult(
                updatedState,
                mappedEffect.map {
                    TestAction(TestAction.Origin.Receive(it))
                },
            )
        }
    }
}

interface ExhaustiveTestStoreScope<State : Any, Action : Any?> {

    val state: State

    suspend fun send(action: Action, assert: ((state: State) -> State)? = null)

    suspend fun receive(action: Action, assert: ((state: State) -> State)? = null)

    suspend fun receive(
        actionPredicate: (Action) -> Boolean,
        assert: ((state: State) -> State)? = null,
    )

    fun advanceTestStoreTimeBy(duration: Duration)
}

interface NonExhaustiveTestStoreScope<State : Any, Action : Any?> : ExhaustiveTestStoreScope<State, Action> {

    val defaultDuration: Duration
    suspend fun skipReceivedActions(count: Int)
    suspend fun awaitEffectsConsumption(timeout: Duration = defaultDuration)
    fun assert(assert: (state: State) -> State)
}

private open class BaseTestStoreScopeImpl<State : Any, Action : Any?>(private val store: TestStore<State, Action>): ExhaustiveTestStoreScope<State, Action> {
    override val state: State
        get() = store.state

    /**
     * See [TestStore.send]
     */
    override suspend fun send(action: Action, assert: ((state: State) -> State)?) =
        store.send(action, assert)

    /**
     * See [TestStore.receive]
     */
    override suspend fun receive(action: Action, assert: ((state: State) -> State)?) =
        store.receive(action, assert)

    /**
     * See [TestStore.receive]
     */
    override suspend fun receive(
        actionPredicate: (Action) -> Boolean,
        assert: ((state: State) -> State)?,
    ) = store.receive(actionPredicate, assert)

    /**
     * Advances the time in the associated TestStore's coroutine scope.
     *
     * @param duration The duration by which to advance the time.
     */
    override fun advanceTestStoreTimeBy(duration: Duration) {
        store.testCoroutineScope.advanceTimeBy(duration)
    }
}

/**
 * A scope for interacting with the TestStore during testing.
 * See [TestStore.test].
 */
private class ExhaustiveTestStoreScopeImpl<State : Any, Action : Any?>(store: TestStore<State, Action>) :
    ExhaustiveTestStoreScope<State, Action>, BaseTestStoreScopeImpl<State, Action>(store)

private class NonExhaustiveTestStoreScopeImpl<State : Any, Action : Any?>(private val store: TestStore<State, Action>) :
    NonExhaustiveTestStoreScope<State, Action>, BaseTestStoreScopeImpl<State, Action>(store) {

    override val defaultDuration: Duration
        get() = store.timeout

    /**
     * Skips a specified number of received actions without asserting state changes.
     * Note that skipped received actions still incur in state changes.
     *
     * @param count The number of received actions to skip.
     */
    override suspend fun skipReceivedActions(count: Int) {
        store.skipReceivedActions(count)
    }

    /**
     * Wait for all effects to finish under the real world [timeout] duration.
     * Don't use [timeout] to wait for any virtual time your effects might be taking to complete, use [advanceTestStoreTimeBy] instead.
     * All ignored received actions will be skipped and their state changes integrated into the final state of the store.
     * The final state of the store can be asserted using [assert].
     * See [TestStore.finish]
     */
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

abstract class TestConfig {
    abstract val dispatcherProvider: DispatcherProvider
    abstract val testCoroutineScope: TestScope
    abstract val timeout: Duration
    abstract val logger: Logger
    abstract val reflectionHandler: ReflectionHandler
    abstract val exhaustivity: TestStore.Exhaustivity
}

data class ExhaustiveTestConfig(
    override val dispatcherProvider: DispatcherProvider,
    override val testCoroutineScope: TestScope,
    override val logger: Logger = NoopLogger(),
    override val timeout: Duration = 100.milliseconds,
    override val reflectionHandler: ReflectionHandler = PublicPropertiesReflectionHandler(),
) : TestConfig() {
    override val exhaustivity: TestStore.Exhaustivity = TestStore.Exhaustivity.Exhaustive
}

data class NonExhaustiveTestConfig(
    override val dispatcherProvider: DispatcherProvider,
    override val testCoroutineScope: TestScope,
    override val logger: Logger = NoopLogger(),
    override val timeout: Duration = 100.milliseconds,
    override val reflectionHandler: ReflectionHandler = PublicPropertiesReflectionHandler(),
    val logIgnoredReceivedActions: Boolean = true,
    val logIgnoredStateChanges: Boolean = true,
    val logIgnoredEffects: Boolean = true,
) : TestConfig() {
    override val exhaustivity: TestStore.Exhaustivity
        get() = TestStore.Exhaustivity.NonExhaustive(
            logIgnoredReceivedActions = logIgnoredReceivedActions,
            logIgnoredStateChanges = logIgnoredStateChanges,
            logIgnoredEffects = logIgnoredEffects,
        )
}

/**
 * Point of entry to test reducers exhaustively.
 * Automatically finishes the test after the test body has been executed, asserting that all effects
 * are done all actions have been received.
 *
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
 * Does NOT automatically finish the test after the test body has been executed.
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
