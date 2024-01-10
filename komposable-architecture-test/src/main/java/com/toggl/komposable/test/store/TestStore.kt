package com.toggl.komposable.test.store

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.extensions.createStore
import com.toggl.komposable.extensions.map
import com.toggl.komposable.scope.DispatcherProvider
import io.kotest.matchers.shouldBe
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
import kotlin.reflect.full.memberProperties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

open class TestStore<State : Any, Action : Any?>(
    initialState: () -> State,
    reducer: () -> Reducer<State, Action>,
    dispatcherProvider: DispatcherProvider,
    private val logger: Logger,
    private val reflectionHandler: ReflectionHandler = PublicPropertiesReflectionHandler(),
    val testCoroutineScope: TestScope,
    var timeout: Duration = 100.milliseconds,
    var exhaustivity: Exhaustivity = Exhaustivity.Exhaustive,
) {
    private val store: Store<State, TestReducer.TestAction<Action>>
    private val reducer: TestReducer<State, Action>
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
            dispatcherProvider = dispatcherProvider,
            storeScopeProvider = { testCoroutineScope },
        )
    }

    suspend fun send(action: Action, assert: ((state: State) -> State)? = null) {
        testCoroutineScope.runCurrent()
        if (reducer.receivedActions.isNotEmpty()) {
            throw AssertionError("Cannot send actions after receiving actions")
        }

        val previousState = reducer.state
        store.send(TestReducer.TestAction(TestReducer.TestAction.Origin.Send(action)))
        effectSubscriptionChannel.receiveCatching()
        testCoroutineScope.runCurrent()
        val currentState = reducer.state

        assertStateChange(previousState, currentState, assert)
    }

    suspend fun receive(
        action: Action,
        assert: ((state: State) -> State)? = null,
    ) {
        receive({ it == action }, assert)
    }

    suspend fun receive(
        actionPredicate: (Action) -> Boolean,
        assert: ((state: State) -> State)? = null,
    ) {
        if (reducer.inFlightEffects.isNotEmpty()) {
            awaitActionFromEffects(timeout, actionPredicate)
        }
        receiveAction({ actionPredicate(it) }, assert)
    }

    private fun receiveAction(
        matching: (Action) -> Boolean,
        assert: ((state: State) -> State)?,
    ) {
        testCoroutineScope.runCurrent()
        if (reducer.receivedActions.isEmpty()) {
            throw AssertionError("No action has been received")
        }
        val exhaustivity = exhaustivity
        if (exhaustivity is Exhaustivity.NonExhaustive) {
            if (reducer.receivedActions.none { matching(it.first) }) {
                throw AssertionError("No action matching the predicate was found")
            }

            val skippedActions = mutableListOf<Action>()
            var foundAction = true
            try {
                while (!matching(reducer.receivedActions.first().first)) {
                    val (action, reducedState) = reducer.receivedActions.removeFirst()
                    skippedActions.add(action)
                    reducer.state = reducedState
                }
            } catch (ex: NoSuchElementException) {
                foundAction = false
            }

            if (exhaustivity.logIgnoredReceivedActions) {
                if (skippedActions.isNotEmpty()) {
                    val log = StringBuilder().apply {
                        appendLine("⚠️")
                        appendLine("The following received actions were skipped:")
                        skippedActions.forEach {
                            appendLine("-$it")
                        }
                        appendLine("Total: ${skippedActions.size}")
                    }
                    logger.info(log.toString())
                }
            }

            if (!foundAction) {
                throw AssertionError("No action matching the predicate was found")
            }
        }

        // handle actual expected action
        val (action, reducedState) = reducer.receivedActions.removeFirst()
        if (!matching(action)) {
            throw AssertionError(
                """
                Next action($action) in the queue doesn't match the predicate.
                """.trimIndent(),
            )
        }

        val currentState = reducer.state
        reducer.state = reducedState

        assertStateChange(currentState, reducedState, assert)
        testCoroutineScope.runCurrent()
    }
    private suspend fun awaitActionFromEffects(
        timeout: Duration,
        matching: (Action) -> Boolean,
    ) {
        testCoroutineScope.runCurrent()
        val start = TimeSource.Monotonic.markNow()
        while (currentCoroutineContext().isActive) {
            testCoroutineScope.runCurrent()
            yield()
            when (exhaustivity) {
                Exhaustivity.Exhaustive -> if (reducer.receivedActions.isNotEmpty()) {
                    return
                }

                is Exhaustivity.NonExhaustive -> if (reducer.receivedActions.any { matching(it.first) }) {
                    return
                }
            }
            if (start.elapsedNow() > timeout) {
                if (reducer.inFlightEffects.isEmpty()) {
                    throw AssertionError("No effect in flight was found that could deliver the action, maybe it has been cancelled?")
                } else {
                    throw AssertionError(
                        """
                        There are in-flight effects that could deliver the action but they haven't finished under the ${timeout.inWholeMilliseconds}ms timeout.
                        Try moving the scheduler forward with `testCoroutineScope.advanceTimeBy(timeoutInMillis)` or `testCoroutineScope.advanceUntilIdle()`
                        before trying to receive the action.
                        """.trimIndent(),
                    )
                }
            }
        }
    }

    private fun assertStateChange(
        previousState: State,
        currentState: State,
        assert: ((state: State) -> State)?,
    ) {
        when (val currentExhaustivity = exhaustivity) {
            is Exhaustivity.Exhaustive -> {
                if (assert != null) {
                    currentState shouldBe assert(previousState)
                } else {
                    currentState shouldBe previousState
                }
            }

            is Exhaustivity.NonExhaustive -> {
                if (assert != null) {
                    val fields = previousState::class.memberProperties.toMutableSet()
                    val previousStateModified = assert(previousState)
                    val currentStateModified = assert(currentState)
                    val assertedFieldChanges = reflectionHandler.filterAccessibleProperty(fields) {
                        it.getter.call(previousState) != it.getter.call(previousStateModified) ||
                            it.getter.call(currentState) != it.getter.call(currentStateModified)
                    }
                    if (assertedFieldChanges.isEmpty()) {
                        throw AssertionError("No state changes were detected but assertion was provided")
                    }
                    assertedFieldChanges.forEach {
                        it.getter.call(currentState) shouldBe it.getter.call(previousStateModified)
                    }
                    if (currentExhaustivity.logIgnoredStateChanges) {
                        var changesHappened = false
                        val log = StringBuilder().apply {
                            appendLine("⚠️")
                            appendLine("The following state changes were not asserted:")
                        }
                        reflectionHandler.forEachAccessibleProperty(fields) {
                            if (it.getter.call(previousStateModified) != it.getter.call(currentState)) {
                                changesHappened = true
                                log.appendLine(".${it.name}")
                                log.appendLine("-Before: ${it.getter.call(previousStateModified)}")
                                log.appendLine("-After: ${it.getter.call(currentState)}")
                            }
                        }
                        if (changesHappened) {
                            logger.info(log.toString())
                        }
                    }
                } else {
                    logger.info("No assertion was provided, skipping state assertion (state did ${if (previousState == currentState) "not " else ""}change)")
                }
            }
        }
    }

    suspend fun skipReceivedActions(count: Int) {
        val currentExhaustivity = exhaustivity
        exhaustivity = Exhaustivity.NonExhaustive(
            logIgnoredReceivedActions = false,
            logIgnoredStateChanges = false,
        )
        repeat(count) {
            receive({ true }, null)
        }
        exhaustivity = currentExhaustivity
    }

    suspend fun finish() {
        val timeout = timeout
        if (reducer.inFlightEffects.isNotEmpty()) {
            testCoroutineScope.runCurrent()
            val start = TimeSource.Monotonic.markNow()
            while (currentCoroutineContext().isActive &&
                start.elapsedNow() < timeout &&
                reducer.inFlightEffects.isNotEmpty()
            ) {
                testCoroutineScope.runCurrent()
                yield()
            }
        }
        val exhaustivity = exhaustivity
        if (reducer.inFlightEffects.isNotEmpty()) {
            val error = StringBuilder().apply {
                if (exhaustivity is Exhaustivity.Exhaustive) {
                    appendLine("🚨")
                } else {
                    appendLine("⚠️")
                }
                appendLine("There are still ${reducer.inFlightEffects.size} effects in flight that didn't finish under the ${timeout}ms timeout:")
                reducer.inFlightEffects.forEach {
                    appendLine("-$it")
                }
            }
            when (exhaustivity) {
                is Exhaustivity.Exhaustive -> throw AssertionError(error.toString())
                is Exhaustivity.NonExhaustive -> if (exhaustivity.logIgnoredEffects) {
                    logger.info(error.toString())
                }
            }
        }

        if (reducer.receivedActions.isNotEmpty()) {
            val error = StringBuilder().apply {
                if (exhaustivity is Exhaustivity.Exhaustive) {
                    appendLine("🚨")
                } else {
                    appendLine("⚠️")
                }
                appendLine("There are still ${reducer.receivedActions.size} actions in the queue:")
                reducer.receivedActions.forEach {
                    appendLine("-$it")
                }
            }
            when (exhaustivity) {
                is Exhaustivity.Exhaustive -> throw AssertionError(error.toString())
                is Exhaustivity.NonExhaustive -> if (exhaustivity.logIgnoredReceivedActions) {
                    logger.info(error.toString())
                }
            }
        }
    }
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
                effect.run()
                    .onStart {
                        subChannel.send(Unit)
                        inFlightEffects.add(longLivingEffect)
                    }
                    .onCompletion {
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

sealed class Exhaustivity {
    data object Exhaustive : Exhaustivity()
    data class NonExhaustive(
        val logIgnoredReceivedActions: Boolean = true,
        val logIgnoredStateChanges: Boolean = true,
        val logIgnoredEffects: Boolean = true,
    ) : Exhaustivity()
}

class TestStoreScope<State : Any, Action : Any?>(val store: TestStore<State, Action>) {
    suspend fun send(action: Action, assert: ((state: State) -> State)? = null) =
        store.send(action, assert)

    suspend fun receive(action: Action, assert: ((state: State) -> State)? = null) =
        store.receive(action, assert)

    suspend fun receive(
        actionPredicate: (Action) -> Boolean,
        assert: ((state: State) -> State)? = null,
    ) = store.receive(actionPredicate, assert)

    suspend fun withExhaustivity(
        exhaustivity: Exhaustivity,
        block: suspend TestStoreScope<State, Action>.() -> Unit,
    ) {
        val previousExhaustivity = store.exhaustivity
        store.exhaustivity = exhaustivity
        try {
            block()
        } finally {
            store.exhaustivity = previousExhaustivity
        }
    }

    fun advanceTimeBy(duration: Duration) {
        store.testCoroutineScope.advanceTimeBy(duration)
    }
}

suspend fun <State : Any, Action : Any?> TestStore<State, Action>.test(
    exhaustivity: Exhaustivity = Exhaustivity.Exhaustive,
    testBody: suspend TestStoreScope<State, Action>.() -> Unit,
) {
    this.exhaustivity = exhaustivity
    val scope = TestStoreScope(this)
    testBody(scope)
    finish()
}
