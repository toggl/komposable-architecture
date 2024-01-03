package com.toggl.komposable.test

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.cancellable
import com.toggl.komposable.extensions.merge
import com.toggl.komposable.extensions.withCancelEffect
import com.toggl.komposable.extensions.withEffect
import com.toggl.komposable.extensions.withoutEffect
import com.toggl.komposable.scope.DispatcherProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
        @Test
        fun `merging working`() = runTest {
            val store = TestStore(
                initialState = { Any() },
                reducer = {
                    object : Reducer<Any, MergeTestsAction> {
                        override fun reduce(
                            state: Any,
                            action: MergeTestsAction,
                        ): ReduceResult<Any, MergeTestsAction> {
                            return when (action) {
                                MergeTestsAction.A -> {
                                    return ReduceResult(
                                        state,
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
                                }

                                MergeTestsAction.B1 -> state.withEffect(
                                    Effect.merge(
                                        Effect.fromFlow(flow { emit(MergeTestsAction.B2) }),
                                        Effect.fromFlow(flow { emit(MergeTestsAction.B3) }),
                                    ),
                                )

                                MergeTestsAction.C1 -> {
                                    state.withEffect(
                                        Effect.of(MergeTestsAction.C2, MergeTestsAction.C3),
                                    )
                                }

                                MergeTestsAction.B2,
                                MergeTestsAction.B3,
                                MergeTestsAction.C2,
                                MergeTestsAction.C3,
                                -> state.withoutEffect()

                                MergeTestsAction.D -> state.withCancelEffect(1)
                            }
                        }
                    }
                },
                dispatcherProvider = dispatcherProvider,
                testCoroutineScope = testCoroutineScope,
            )

            store.send(MergeTestsAction.A)

            testScheduler.advanceTimeBy(1000)

            store.receive(MergeTestsAction.B1)
            store.receive(MergeTestsAction.B2)
            store.receive(MergeTestsAction.B3)

            store.receive(MergeTestsAction.C1)
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
            val store = TestStore(
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
            val store = TestStore(
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
            val store = TestStore(
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
            val store = TestStore(
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
}