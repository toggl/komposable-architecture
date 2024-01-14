package com.toggl.komposable.test

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.cancellable
import com.toggl.komposable.extensions.merge
import com.toggl.komposable.extensions.withCancelEffect
import com.toggl.komposable.extensions.withEffect
import com.toggl.komposable.extensions.withSuspendEffect
import com.toggl.komposable.extensions.withoutEffect
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.test.store.TestStore.Exhaustivity
import com.toggl.komposable.test.store.createTestStore
import com.toggl.komposable.test.store.test
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class TestStoreTests {
    open class BaseTestStoreTest {
        private val testDispatcher = StandardTestDispatcher()
        val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
        val testCoroutineScope = TestScope(testDispatcher)

        @BeforeEach
        fun beforeTest() {
            Dispatchers.setMain(testDispatcher)
        }
    }

    private enum class MergeTestsAction {
        A, B1, B2, B3, C1, C2, C3, D
    }

    @Nested
    inner class MergeTests : BaseTestStoreTest() {

        private val reducer = Reducer<Any, MergeTestsAction> { state, action ->
            when (action) {
                MergeTestsAction.A -> state.withEffect(
                    Effect.merge(
                        Effect.fromFlow(
                            flow {
                                delay(1000)
                                emit(MergeTestsAction.B1)
                                emit(MergeTestsAction.C1)
                            },
                        ),
                        Effect.fromFlow<MergeTestsAction>(
                            flow {
                                delay(Duration.INFINITE)
                            },
                        ).cancellable(1, true),
                    ),
                )

                MergeTestsAction.B1 -> state.withEffect(
                    Effect.merge(
                        Effect.fromFlow(flow { emit(MergeTestsAction.B2) }),
                        Effect.fromFlow(flow { emit(MergeTestsAction.B3) }),
                    ),
                )

                MergeTestsAction.C1 -> state.withEffect(
                    Effect.merge(
                        Effect.fromFlow(flow { emit(MergeTestsAction.C2) }),
                        Effect.fromFlow(flow { emit(MergeTestsAction.C3) }),
                    ),
                )

                MergeTestsAction.B2,
                MergeTestsAction.B3,
                MergeTestsAction.C2,
                MergeTestsAction.C3,
                -> state.withoutEffect()

                MergeTestsAction.D -> state.withCancelEffect(1)
            }
        }

        @Test
        fun `merged flows are merged together as they are reduced`() = runTest {
            val store = createTestStore(
                initialState = { Any() },
                reducer = { reducer },
                dispatcherProvider = dispatcherProvider,
                testCoroutineScope = testCoroutineScope,
            )

            store.send(MergeTestsAction.A)

            testScheduler.advanceTimeBy(1000)

            // B1 and C1 are emitted at the same time
            // KA's implementation does not guarantee that an action's effect will be handled right after they've been emitted
            store.receive(MergeTestsAction.B1)
            // Note that B1's effect has started emitting yet, so C1 has time to be received
            store.receive(MergeTestsAction.C1)

            // B2 and B3 are merged into a single flow
            store.receive(MergeTestsAction.B2)
            store.receive(MergeTestsAction.B3)

            // C2 and C3 are merged into a single flow
            store.receive(MergeTestsAction.C2)
            store.receive(MergeTestsAction.C3)

            store.send(MergeTestsAction.D)
        }
    }

    sealed class AsyncEffectsAction {
        data object Tap : AsyncEffectsAction()
        data class Set(val value: Int) : AsyncEffectsAction()
    }

    @Nested
    inner class AsyncEffects : BaseTestStoreTest() {
        @Test
        fun `async effects are received`() = runTest {
            val store = createTestStore(
                initialState = { 0 },
                reducer = {
                    Reducer<Int, AsyncEffectsAction> { state, action ->
                        when (action) {
                            is AsyncEffectsAction.Tap -> state.withEffect(
                                Effect.fromSuspend { AsyncEffectsAction.Set(69) },
                            )

                            is AsyncEffectsAction.Set -> action.value.withoutEffect()
                        }
                    }
                },
                dispatcherProvider = dispatcherProvider,
                testCoroutineScope = testCoroutineScope,
            )

            store.send(AsyncEffectsAction.Tap)
            store.receive(AsyncEffectsAction.Set(69)) {
                69
            }
        }
    }

    sealed class StateChangesAction {
        data object Increment : StateChangesAction()
        data class Changed(val from: Int, val to: Int) : StateChangesAction()
    }

    data class StateChangesState(val count: Int, val isChanging: Boolean)

    @Nested
    inner class StateChanges : BaseTestStoreTest() {
        @Test
        fun `state changes must be verified`() = runTest {
            val store = createTestStore(
                initialState = { StateChangesState(0, false) },
                reducer = {
                    Reducer<StateChangesState, StateChangesAction> { state, action ->
                        when (action) {
                            StateChangesAction.Increment -> state.copy(
                                isChanging = true,
                            ).withEffect(
                                Effect.of(
                                    StateChangesAction.Changed(
                                        state.count,
                                        state.count + 1,
                                    ),
                                ),
                            )

                            is StateChangesAction.Changed -> {
                                state.copy(
                                    isChanging = false,
                                    count = if (action.from == state.count) action.to else state.count,
                                ).withoutEffect()
                            }
                        }
                    }
                },
                dispatcherProvider = dispatcherProvider,
                testCoroutineScope = testCoroutineScope,
            )

            store.send(StateChangesAction.Increment) {
                it.copy(isChanging = true)
            }
            store.receive(StateChangesAction.Changed(0, 1)) {
                it.copy(count = 1, isChanging = false)
            }

            shouldThrow<AssertionError> {
                store.send(StateChangesAction.Increment) {
                    it.copy(isChanging = false)
                }
            }

            shouldThrow<AssertionError> {
                store.receive(StateChangesAction.Changed(1, 2)) {
                    it.copy(count = 1000, isChanging = true)
                }
            }
        }
    }

    enum class ActionMatchingAction { Noop, Finished }

    @Nested
    inner class ActionMatching : BaseTestStoreTest() {
        @Test
        fun `matching predicates are executed`() = runTest {
            val store = createTestStore(
                initialState = { },
                reducer = {
                    Reducer<Unit, ActionMatchingAction> { state, action ->
                        when (action) {
                            ActionMatchingAction.Noop -> state.withEffect(ActionMatchingAction.Finished)
                            ActionMatchingAction.Finished -> state.withoutEffect()
                        }
                    }
                },
                dispatcherProvider = dispatcherProvider,
                testCoroutineScope = testCoroutineScope,
            )
            store.send(ActionMatchingAction.Noop)
            store.receive({ action -> action == ActionMatchingAction.Finished })

            store.send(ActionMatchingAction.Noop)
            shouldThrow<AssertionError> {
                store.receive(ActionMatchingAction.Noop)
            }

            store.send(ActionMatchingAction.Noop)
            store.receive(ActionMatchingAction.Finished)
            store.send(ActionMatchingAction.Noop)
            shouldThrow<AssertionError> {
                store.receive({ action -> action == ActionMatchingAction.Noop })
            }
        }
    }

    enum class StateAccessAction { A, B, C, D }

    @Nested
    inner class StateAccess : BaseTestStoreTest() {
        @Test
        fun `test store state is updated after assertion`() = runTest {
            val store = createTestStore(
                initialState = { 0 },
                reducer = {
                    Reducer<Int, StateAccessAction> { state, action ->
                        when (action) {
                            StateAccessAction.A -> (state + 1).withEffect(
                                StateAccessAction.B,
                                StateAccessAction.C,
                                StateAccessAction.D,
                            )

                            StateAccessAction.B,
                            StateAccessAction.C,
                            StateAccessAction.D,
                            -> (state + 1).withoutEffect()
                        }
                    }
                },
                dispatcherProvider = dispatcherProvider,
                testCoroutineScope = testCoroutineScope,
            )

            store.send(StateAccessAction.A) {
                it shouldBe 0
                1
            }
            store.state shouldBe 1
            store.receive(StateAccessAction.B) {
                it shouldBe 1
                2
            }
            store.state shouldBe 2
            store.receive(StateAccessAction.C) {
                it shouldBe 2
                3
            }
            store.receive(StateAccessAction.D) {
                it shouldBe 3
                4
            }
            store.state shouldBe 4
        }
    }

    data class ExhaustivityTestState(
        val count: Int,
        private val isEven: Boolean,
    )

    enum class ExhaustivityTestAction { Increment, IncrementTap }

    @Nested
    inner class ExhaustivityTests : BaseTestStoreTest() {
        private val store = createTestStore(
            initialState = { ExhaustivityTestState(0, true) },
            reducer = {
                Reducer<ExhaustivityTestState, ExhaustivityTestAction> { state, action ->
                    when (action) {
                        ExhaustivityTestAction.Increment -> state.copy(
                            count = state.count + 1,
                            isEven = (state.count + 1) % 2 == 0,
                        ).withoutEffect()

                        ExhaustivityTestAction.IncrementTap -> state.withSuspendEffect {
                            delay(1001)
                            ExhaustivityTestAction.Increment
                        }
                    }
                }
            },
            dispatcherProvider = dispatcherProvider,
            testCoroutineScope = testCoroutineScope,
        ).apply {
            exhaustivity = Exhaustivity.NonExhaustive()
        }

        @Nested
        inner class NonExhaustive {
            @Test
            fun `partial changes are verified`() = runTest {
                store.exhaustivity = Exhaustivity.NonExhaustive()
                store.send(ExhaustivityTestAction.Increment) {
                    it.copy(count = 1) // not verifying isEven
                }

                store.send(ExhaustivityTestAction.Increment) {
                    it.copy(isEven = true) // not verifying count
                }

                store.send(ExhaustivityTestAction.IncrementTap)
                testCoroutineScope.advanceTimeBy(1001)
                store.receive(ExhaustivityTestAction.Increment) // not verifying either
            }

            @Test
            fun `cumulative changes can be verified at the end`() = runTest {
                store.test(
                    exhaustivity = Exhaustivity.NonExhaustive(
                        logIgnoredStateChanges = false,
                        logIgnoredReceivedActions = false,
                    ),
                ) {
                    repeat(10) {
                        send(ExhaustivityTestAction.IncrementTap)
                    }
                    advanceTimeBy(1001)
                }
                store.assert { state ->
                    state.copy(count = 10)
                }
            }

            @Test
            fun `finish does not wait for effects completion`() = runTest {
                store.exhaustivity = Exhaustivity.NonExhaustive()

                store.send(ExhaustivityTestAction.IncrementTap)
                testCoroutineScope.advanceTimeBy(500)

                shouldNotThrow<AssertionError> {
                    // did not enough for effect but still triggered finish
                    store.finish()
                }
            }
        }

        @Nested
        inner class Exhaustive {
            @Test
            fun `must assert everything`() = runTest {
                store.exhaustivity = Exhaustivity.Exhaustive
                withClue("Did not verify state change") {
                    shouldThrow<AssertionError> {
                        // did not verify state change
                        store.send(ExhaustivityTestAction.Increment)
                    }
                }

                store.send(ExhaustivityTestAction.IncrementTap)
                testCoroutineScope.advanceTimeBy(1001)
                withClue("Did not receive action") {
                    shouldThrow<AssertionError> {
                        // did not receive action
                        store.send(ExhaustivityTestAction.IncrementTap)
                    }
                }
                store.skipReceivedActions(count = 1)

                withClue("Did not wait for effect") {
                    shouldThrow<AssertionError> {
                        // did not verify state change
                        store.send(ExhaustivityTestAction.IncrementTap)
                        testCoroutineScope.advanceTimeBy(500)
                        store.finish()
                    }
                }
            }
        }
    }
}
