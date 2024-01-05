package com.toggl.komposable.test

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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.yield
import java.util.UUID
import java.util.logging.Logger
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible

class TestStore<State : Any, Action : Any?>(
    initialState: () -> State,
    reducer: () -> Reducer<State, Action>,
    dispatcherProvider: DispatcherProvider,
    val testCoroutineScope: TestScope,
) {
    private val logger: Logger = Logger.getLogger(TestStore::class.java.name).apply { }
    private val store: Store<State, TestReducer.TestAction<Action>>
    val reducer: TestReducer<State, Action>
    val state: State
        get() = reducer.state

    private val effectSubscriptionChannel = Channel<Unit>(capacity = BUFFERED)

    var exhaustivity: Exhaustivity = Exhaustivity.Exhaustive
    var timeoutInMillis: Long = 100

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
        if (reducer.receivedActions.isNotEmpty()) {
            throw IllegalStateException("Cannot send actions after receiving actions")
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
            waitActionFromEffects(timeoutInMillis, actionPredicate)
        }
        receiveAction({ actionPredicate(it) }, assert)
    }

    private suspend fun receiveAction(
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

            if (exhaustivity.logIgnoredReceivedEvents) {
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

    private suspend fun waitActionFromEffects(timeoutInMillis: Long, matching: (Action) -> Boolean) {
        testCoroutineScope.runCurrent()
        val start = System.currentTimeMillis()
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
            if (System.currentTimeMillis() - start > timeoutInMillis) {
                if (reducer.inFlightEffects.isEmpty()) {
                    throw AssertionError("No effect in flight was found that could deliver the action, maybe it has been cancelled?")
                } else {
                    throw AssertionError(
                        """
                        There are in-flight effects that could deliver the action but they haven't finished under the ${timeoutInMillis}ms timeout.
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
                    val fields =
                        previousState::class.members.filterIsInstance<KProperty<*>>().toMutableSet()
                    val previousStateModified = assert(previousState)
                    val currentStateModified = assert(currentState)
                    val assertedFieldChanges = fields.filter {
                        it.isAccessible = true
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
                        fields.forEach {
                            it.isAccessible = true
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
}

class TestReducer<State, Action>(
    private val innerReducer: Reducer<State, Action>,
    var state: State,
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
        val id: UUID = UUID.randomUUID(),
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
        val logIgnoredReceivedEvents: Boolean = true,
        val logIgnoredStateChanges: Boolean = true,
    ) : Exhaustivity()
}

class TestStoreScope(val store: TestStore<*, *>)

suspend fun <State : Any, Action : Any?> TestStore<State, Action>.test(testBody: suspend TestStoreScope.() -> Unit) {
    val scope = TestStoreScope(this)
    testBody(scope)
}
