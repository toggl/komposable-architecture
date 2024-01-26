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
import com.toggl.komposable.test.store.ExhaustiveTestConfig
import com.toggl.komposable.test.store.NonExhaustiveTestConfig
import com.toggl.komposable.test.store.test
import com.toggl.komposable.test.utils.JavaLogger
import com.toggl.komposable.test.utils.JvmReflectionHandler
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
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class TestStoreTests {
    open class BaseTestStoreTest {
        private val testDispatcher = StandardTestDispatcher()
        val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
        val testCoroutineScope = TestScope(testDispatcher)
        val exhaustiveConfig = ExhaustiveTestConfig(
            dispatcherProvider,
            testCoroutineScope,
            JavaLogger(Logger.getLogger("TestStore")),
            reflectionHandler = JvmReflectionHandler(),
        )
        val nonExhaustiveTestConfig = NonExhaustiveTestConfig(
            dispatcherProvider,
            testCoroutineScope,
            JavaLogger(Logger.getLogger("TestStore")),
            reflectionHandler = JvmReflectionHandler(),
            logIgnoredReceivedActions = true,
            logIgnoredStateChanges = true,
            logIgnoredEffects = true,
        )

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
            reducer.test(Any(), exhaustiveConfig) {
                send(MergeTestsAction.A)

                testScheduler.advanceTimeBy(1000)

                // B1 and C1 are emitted at the same time
                // KA's implementation does not guarantee that an action's effect will be handled right after they've been emitted
                receive(MergeTestsAction.B1)
                // Note that B1's effect hasn't started emitting yet, so C1 has time to be received
                receive(MergeTestsAction.C1)

                // B2 and B3 are merged into a single flow
                receive(MergeTestsAction.B2)
                receive(MergeTestsAction.B3)

                // C2 and C3 are merged into a single flow
                receive(MergeTestsAction.C2)
                receive(MergeTestsAction.C3)

                send(MergeTestsAction.D)
            }
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
            val reducer = Reducer<Int, AsyncEffectsAction> { state, action ->
                when (action) {
                    is AsyncEffectsAction.Tap -> state.withEffect(
                        Effect.fromSuspend { AsyncEffectsAction.Set(69) },
                    )

                    is AsyncEffectsAction.Set -> action.value.withoutEffect()
                }
            }

            reducer.test(0, exhaustiveConfig) {
                send(AsyncEffectsAction.Tap)
                receive(AsyncEffectsAction.Set(69)) { 69 }
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
            val initialState = StateChangesState(0, false)
            val reducer = Reducer<StateChangesState, StateChangesAction> { state, action ->
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

            reducer.test(initialState, exhaustiveConfig) {
                send(StateChangesAction.Increment) {
                    it.copy(isChanging = true)
                }
                receive(StateChangesAction.Changed(0, 1)) {
                    it.copy(count = 1, isChanging = false)
                }

                shouldThrow<AssertionError> {
                    send(StateChangesAction.Increment) {
                        it.copy(isChanging = false)
                    }
                }

                shouldThrow<AssertionError> {
                    receive(StateChangesAction.Changed(1, 2)) {
                        it.copy(count = 1000, isChanging = true)
                    }
                }
            }
        }
    }

    enum class ActionMatchingAction { Noop, Finished }

    @Nested
    inner class ActionMatching : BaseTestStoreTest() {
        @Test
        fun `matching predicates are executed`() = runTest {
            val reducer = Reducer<Unit, ActionMatchingAction> { state, action ->
                when (action) {
                    ActionMatchingAction.Noop -> state.withEffect(ActionMatchingAction.Finished)
                    ActionMatchingAction.Finished -> state.withoutEffect()
                }
            }

            reducer.test(Unit, exhaustiveConfig) {
                send(ActionMatchingAction.Noop)
                receive({ action -> action == ActionMatchingAction.Finished })

                send(ActionMatchingAction.Noop)
                shouldThrow<AssertionError> {
                    receive(ActionMatchingAction.Noop)
                }

                send(ActionMatchingAction.Noop)
                receive(ActionMatchingAction.Finished)
                send(ActionMatchingAction.Noop)
                shouldThrow<AssertionError> {
                    receive({ action -> action == ActionMatchingAction.Noop })
                }
            }
        }
    }

    enum class StateAccessAction { A, B, C, D }

    @Nested
    inner class StateAccess : BaseTestStoreTest() {
        @Test
        fun `test store state is updated after assertion`() = runTest {
            val initialState = 0
            val reducer = Reducer<Int, StateAccessAction> { state, action ->
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

            reducer.test(initialState, exhaustiveConfig) {
                send(StateAccessAction.A) {
                    it shouldBe 0
                    1
                }
                state shouldBe 1
                receive(StateAccessAction.B) {
                    it shouldBe 1
                    2
                }
                state shouldBe 2
                receive(StateAccessAction.C) {
                    it shouldBe 2
                    3
                }
                receive(StateAccessAction.D) {
                    it shouldBe 3
                    4
                }
                state shouldBe 4
            }
        }
    }

    data class ExhaustivityTestState(
        val count: Int,
        private val isEven: Boolean,
    )

    enum class ExhaustivityTestAction { Increment, IncrementTap }

    @Nested
    inner class ExhaustivityTests : BaseTestStoreTest() {

        private val initialState = ExhaustivityTestState(0, true)
        private val reducer =
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

        @Nested
        inner class NonExhaustive {
            @Test
            fun `partial changes are verified`() = runTest {
                reducer.test(initialState, nonExhaustiveTestConfig) {
                    send(ExhaustivityTestAction.Increment) {
                        it.copy(count = 1) // not verifying isEven
                    }

                    send(ExhaustivityTestAction.Increment) {
                        it.copy(isEven = true) // not verifying count
                    }

                    send(ExhaustivityTestAction.IncrementTap)
                    testCoroutineScope.advanceTimeBy(1001)
                    receive(ExhaustivityTestAction.Increment) // not verifying either
                }
            }

            @Test
            fun `cumulative changes can be verified at the end`() = runTest {
                reducer.test(
                    initialState,
                    nonExhaustiveTestConfig.copy(
                        logIgnoredStateChanges = false,
                        logIgnoredReceivedActions = false,
                    ),
                ) {
                    repeat(10) {
                        send(ExhaustivityTestAction.IncrementTap)
                    }
                    advanceTestStoreTimeBy(1001.milliseconds)
                    awaitEffectsConsumption()
                    assert { state ->
                        state.copy(count = 10)
                    }
                }
            }

            @Test
            fun `finish does not wait for effects completion`() = runTest {
                reducer.test(initialState, nonExhaustiveTestConfig) {
                    send(ExhaustivityTestAction.IncrementTap)
                    advanceTestStoreTimeBy(500.milliseconds)

                    shouldNotThrow<AssertionError> {
                        // did not advance time enough for effect to finish but still tried to wait for it
                        awaitEffectsConsumption()
                    }
                }
            }
        }

        @Nested
        inner class Exhaustive {
            @Test
            fun `must assert everything`() = runTest {
                shouldThrow<AssertionError> { // did not wait for effects to finish
                    reducer.test(initialState, exhaustiveConfig) {
                        withClue("Did not verify state change") {
                            shouldThrow<AssertionError> {
                                // did not verify state change
                                send(ExhaustivityTestAction.Increment)
                            }
                        }

                        send(ExhaustivityTestAction.IncrementTap)
                        advanceTestStoreTimeBy(1001.milliseconds)
                        withClue("Did not receive action") {
                            shouldThrow<AssertionError> {
                                // did not receive action
                                send(ExhaustivityTestAction.IncrementTap)
                            }
                        }
                        receive(ExhaustivityTestAction.Increment) {
                            it.copy(count = 2, isEven = true)
                        }

                        send(ExhaustivityTestAction.IncrementTap)
                        advanceTestStoreTimeBy(500.milliseconds)
                    }
                }
            }
        }
    }
}
