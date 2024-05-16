package com.toggl.komposable.effect

import app.cash.turbine.turbineScope
import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.debounce
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.test.store.ExhaustiveTestConfig
import com.toggl.komposable.test.store.test
import com.toggl.komposable.test.utils.JavaLogger
import com.toggl.komposable.test.utils.JvmReflectionHandler
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

class EffectDebounceTests {
    @Test
    fun `effects debounce on stores`() = runTest {
        val reducer = Reducer<Int, String> { state, action ->
            when (action) {
                "tap_add" -> ReduceResult(
                    state,
                    Effect.fromSuspend {
                        "add"
                    }.debounce("debounce", 100),
                )

                "add" -> ReduceResult(state + 1, Effect.none())
                else -> ReduceResult(state, Effect.none())
            }
        }
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
        val testCoroutineScope = TestScope(testDispatcher)
        val exhaustiveConfig = ExhaustiveTestConfig(
            dispatcherProvider,
            testCoroutineScope,
            JavaLogger(Logger.getLogger("TestStore")),
            reflectionHandler = JvmReflectionHandler(),
        )

        reducer.test(0, exhaustiveConfig) {
            send("tap_add")
            this.advanceTestStoreTimeBy(50.milliseconds)
            send("tap_add")
            this.advanceTestStoreTimeBy(50.milliseconds)
            send("tap_add")
            this.advanceTestStoreTimeBy(100.milliseconds)
            this.receive("add") {
                1
            }
            send("tap_add")
            this.advanceTestStoreTimeBy(100.milliseconds)
            this.receive("add") {
                2
            }
        }
    }

    @Test
    fun `effects debounce`() = runTest {
        val someState = MutableStateFlow(emptySet<Int>())
        fun doTheThing(value: Set<Int>): Effect<Set<Int>> {
            return Effect.fromSuspend {
                value
            }.debounce("debouncer", 100)
        }

        turbineScope {
            val effect1 = doTheThing(setOf(1)).run()
                .onEach { someState.value = it }
                .testIn(this)
            delay(50)
            effect1.expectNoEvents()
            someState.value shouldBe emptySet()

            val effect2 = doTheThing(setOf(2)).run()
                .onEach { someState.value = it }
                .testIn(this)
            delay(50)
            effect1.awaitError()
            effect2.expectNoEvents()
            someState.value shouldBe emptySet()

            val effect3 = doTheThing(setOf(3)).run()
                .onEach { someState.value = it }
                .testIn(this)
            delay(50)
            effect2.awaitError()
            effect3.expectNoEvents()
            someState.value shouldBe emptySet()

            delay(50)
            effect3.awaitItem() shouldBe setOf(3)
            effect3.awaitComplete()
            someState.value shouldBe setOf(3)
        }
    }

    @Test
    fun `effects do not affect their internal flows`() = runTest {
        val someState = MutableStateFlow(emptySet<Int>())
        fun emitThings(list: List<Int>): Effect<Int> {
            return Effect.fromFlow(
                flow {
                    for (item in list) {
                        delay(50)
                        emit(item)
                    }
                },
            ).debounce("debouncer", 100)
        }

        turbineScope {
            val effect1 = emitThings(listOf(1, 2, 3)).run()
                .onEach { someState.value += it }
                .testIn(this)

            // effect is being debounced for 100ms
            delay(100)
            effect1.expectNoEvents()

            delay(50)
            // internal flow took 50ms to emit (1)
            effect1.awaitItem() shouldBe 1
            someState.value shouldBe setOf(1)

            val effect2 = emitThings(listOf(4, 5, 6, 7)).run()
                .onEach { someState.value += it }
                .testIn(this)
            // effect1 got cancelled in favor of effect1
            effect1.awaitError()
            delay(100)
            effect2.expectNoEvents()

            // internal flow took 50ms to emit (4)
            delay(50)
            effect2.awaitItem() shouldBe 4
            delay(50)
            effect2.awaitItem() shouldBe 5
            delay(50)
            effect2.awaitItem() shouldBe 6
            delay(50)
            effect2.awaitItem() shouldBe 7
            effect2.awaitComplete()
            someState.value shouldBe setOf(1, 4, 5, 6, 7)
        }
    }
}
