package com.toggl.komposable.store

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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import java.util.UUID

class TestStore<State, Action : Any?>(
    initialState: () -> State,
    reducer: () -> Reducer<State, Action>,
    dispatcherProvider: DispatcherProvider,
    val testCoroutineScope: TestScope
) {
    private val store: Store<State, TestReducer.TestAction<Action>>
    val reducer: TestReducer<State, Action>
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
        if (reducer.receivedActions.isNotEmpty()) {
            throw IllegalStateException("Cannot send actions after receiving actions")
        }

        val previousState = reducer.state
        store.send(TestReducer.TestAction(TestReducer.TestAction.Origin.Send(action)))
        effectSubscriptionChannel.receiveCatching()
        testCoroutineScope.runCurrent()
        val currentState = reducer.state

        if (assert != null) {
            currentState shouldBe assert(previousState)
        }
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
        if (reducer.inFlightEffects.isEmpty()) {
            receiveAction(actionPredicate, assert)
        } else {
            testCoroutineScope.runCurrent()
            testCoroutineScope.advanceUntilIdle()
            receiveAction(actionPredicate, assert)
            // wait for the in-flight effect to finish
        }
    }

    private fun receiveAction(
        matching: (Action) -> Boolean,
        assert: ((state: State) -> State)?,
    ) {
        val action = reducer.receivedActions.firstOrNull { matching(it.first) }?.first
            ?: throw AssertionError("No action matching the predicate was received")

        val reducedState = reducer.receivedActions.find { it.first == action }?.run {
            reducer.receivedActions.remove(this)
            this.second
        } ?: throw AssertionError("No state matching the action was found")

        val currentState = reducer.state
        reducer.state = reducedState

        if (assert != null) {
            assert(currentState) shouldBe reducedState
        }
        testCoroutineScope.runCurrent()
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
