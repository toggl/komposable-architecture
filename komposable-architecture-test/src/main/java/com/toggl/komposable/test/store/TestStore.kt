package com.toggl.komposable.test.store

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.extensions.createStore
import com.toggl.komposable.extensions.map
import com.toggl.komposable.test.utils.Logger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.yield
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Internal utility for managing and testing state changes in the Komposable architecture.
 *
 * @param initialState A lambda function providing the initial state of the store.
 * @param reducer A lambda function providing the reducer that will power the test store.
 * @param config A [TestConfig] object that configures the testing process.
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
    private val exhaustivity: TestExhaustivity = config.exhaustivity
    private var tempExhaustivity: TestExhaustivity? = null
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

    private suspend fun withTempExhaustivity(
        exhaustivity: TestExhaustivity,
        block: suspend () -> Unit,
    ) {
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

    internal suspend fun receive(action: Action, assert: ((state: State) -> State)? = null) {
        receive({ it == action }, assert)
    }

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

    internal suspend fun skipReceivedActions(count: Int) {
        withTempExhaustivity(
            TestExhaustivity.NonExhaustive(
                logIgnoredReceivedActions = false,
                logIgnoredStateChanges = false,
                logIgnoredEffects = false,
            ),
        ) {
            repeat(count) {
                receive({ true }, null)
            }
        }
    }

    /**
     * Finishes the testing process by ensuring all effects are done and all fired actions have been
     * received and their state changes asserted.
     *
     * When running tests with [TestExhaustivity.NonExhaustive], this method may be called to wait for all effects
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
     * Asserts the current state of the store. Only useful when running tests with [TestExhaustivity.NonExhaustive].
     * See [TestStore.finish].
     */
    internal fun assert(assert: (state: State) -> State) {
        assertionRunner.assertStateChange(reducer.state, reducer.state, assert)
    }

    /**
     * A reducer that wraps the original reducer and keeps track of the state and actions received.
     * @param innerReducer The original reducer.
     * @param state The initial state of the store.
     * @param testCoroutineScope The coroutine scope of the test.
     * @param subChannel A channel used to signal that an effect has been subscribed to.
     */
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
