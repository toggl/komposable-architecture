package com.toggl.komposable.test

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.effectOf
import com.toggl.komposable.extensions.mutateWithoutEffects
import com.toggl.komposable.extensions.noEffect
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.lang.AssertionError
import kotlin.IllegalStateException

data class TestState(val title: String)
enum class TestAction { FirstAction, SecondAction }
class TestEffect : Effect<TestAction> {
    override suspend fun execute(): TestAction = TestAction.SecondAction
}

class ReducerTestExtensionsTests {

    private val initState = TestState("")
    private val inputAction = TestAction.FirstAction
    private val returnedEffects = effectOf(TestEffect())

    @Test
    fun `testReduce calls the right methods`() = runTest {
        val testCase: suspend (TestState, List<Effect<TestAction>>) -> Unit = mockk(relaxed = true)
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns returnedEffects
        }

        reducer.testReduce(initState, inputAction, testCase)

        val mutableStateSlot = slot<Mutable<TestState>>()
        coVerify {
            reducer.reduce(capture(mutableStateSlot), inputAction)
            testCase.invoke(initState, returnedEffects)
        }
        mutableStateSlot.captured() shouldBe initState
    }

    @Test
    fun `testReduceState calls the right methods`() = runTest {
        val testCase: suspend (TestState) -> Unit = mockk(relaxed = true)
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns returnedEffects
        }

        reducer.testReduceState(initState, inputAction, testCase)

        val mutableStateSlot = slot<Mutable<TestState>>()
        coVerify {
            reducer.reduce(capture(mutableStateSlot), inputAction)
            testCase.invoke(initState)
        }
        mutableStateSlot.captured() shouldBe initState
    }

    @Test
    fun `testReduceEffects calls the right methods`() = runTest {
        val testCase: suspend (List<Effect<TestAction>>) -> Unit = mockk(relaxed = true)
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns returnedEffects
        }

        reducer.testReduceEffects(initState, inputAction, testCase)

        val mutableStateSlot = slot<Mutable<TestState>>()
        coVerify {
            reducer.reduce(capture(mutableStateSlot), inputAction)
            testCase.invoke(returnedEffects)
        }
        mutableStateSlot.captured() shouldBe initState
    }

    @Test
    fun `testReduceNoEffects calls the right methods`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns noEffect()
        }

        reducer.testReduceNoEffects(initState, inputAction)

        val mutableStateSlot = slot<Mutable<TestState>>()
        coVerify {
            reducer.reduce(capture(mutableStateSlot), inputAction)
        }
        mutableStateSlot.captured() shouldBe initState
    }

    @Test
    fun `testReduceNoEffects should fail when some effects are returned`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns returnedEffects
        }

        shouldThrow<AssertionError> {
            reducer.testReduceNoEffects(initState, inputAction)
        }
    }

    @Test
    fun `testReduceNoOp calls the right methods`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns noEffect()
        }

        reducer.testReduceNoOp(initState, inputAction)

        val mutableStateSlot = slot<Mutable<TestState>>()
        verify {
            reducer.reduce(capture(mutableStateSlot), inputAction)
        }
        mutableStateSlot.captured() shouldBe initState
    }

    @Test
    fun `testReduceNoOp fails when some effects are returned`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns returnedEffects
        }

        shouldThrow<AssertionError> {
            reducer.testReduceNoOp(initState, inputAction)
        }
    }

    @Test
    fun `testReduceNoOp fails when state has been changed`() = runTest {
        val reducer =
            Reducer<TestState, TestAction> { s, _ -> s.mutateWithoutEffects { TestState("Changed") } }

        shouldThrow<AssertionError> {
            reducer.testReduceNoOp(initState, inputAction)
        }
    }

    @Test
    fun `testReduceException calls the right methods`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } throws IllegalStateException()
        }

        reducer.testReduceException(initState, inputAction, IllegalStateException::class)

        val mutableStateSlot = slot<Mutable<TestState>>()
        coVerify {
            reducer.reduce(capture(mutableStateSlot), inputAction)
        }
        mutableStateSlot.captured() shouldBe initState
    }

    @Test
    fun `testReduceException fails when non expected exception is thrown`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } throws IllegalAccessException()
        }

        shouldThrow<AssertionError> {
            reducer.testReduceException(initState, inputAction, IllegalStateException::class)
        }
    }

    @Test
    fun `testReduceException fails when no exception is thrown`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns noEffect()
        }

        shouldThrow<AssertionError> {
            reducer.testReduceException(initState, inputAction, IllegalStateException::class)
        }
    }
}
