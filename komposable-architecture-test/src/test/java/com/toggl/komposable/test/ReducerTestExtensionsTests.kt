package com.toggl.komposable.test

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.lang.AssertionError
import kotlin.IllegalStateException

data class TestState(val title: String)
enum class TestAction { FirstAction, SecondAction }
class TestEffect : Effect<TestAction> {
    override fun run(): Flow<TestAction> = flowOf(TestAction.SecondAction)
}

class ReducerTestExtensionsTests {

    private val initState = TestState("")
    private val inputAction = TestAction.FirstAction
    private val returnedEffects = TestEffect()

    @Test
    fun `testReduce calls the right methods`() = runTest {
        val testCase: suspend (TestState, Effect<TestAction>) -> Unit = mockk(relaxed = true)
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns ReduceResult(initState, returnedEffects)
        }

        reducer.testReduce(initState, inputAction, testCase)

        coVerify {
            reducer.reduce(initState, inputAction)
            testCase.invoke(initState, returnedEffects)
        }
    }

    @Test
    fun `testReduceState calls the right methods`() = runTest {
        val testCase: suspend (TestState) -> Unit = mockk(relaxed = true)
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns ReduceResult(initState, returnedEffects)
        }

        reducer.testReduceState(initState, inputAction, testCase)

        coVerify {
            reducer.reduce(initState, inputAction)
            testCase.invoke(initState)
        }
    }

    @Test
    fun `testReduceEffects calls the right methods`() = runTest {
        val testCase: suspend (Effect<TestAction>) -> Unit = mockk(relaxed = true)
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns ReduceResult(initState, returnedEffects)
        }

        reducer.testReduceEffect(initState, inputAction, testCase)

        coVerify {
            reducer.reduce(initState, inputAction)
            testCase.invoke(returnedEffects)
        }
    }

    @Test
    fun `testReduceNoEffects calls the right methods`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns ReduceResult(initState, NoEffect)
        }

        reducer.testReduceNoEffect(initState, inputAction)

        coVerify {
            reducer.reduce(initState, inputAction)
        }
    }

    @Test
    fun `testReduceNoEffects should fail when some effects are returned`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns ReduceResult(initState, returnedEffects)
        }

        shouldThrow<AssertionError> {
            reducer.testReduceNoEffect(initState, inputAction)
        }
    }

    @Test
    fun `testReduceNoOp calls the right methods`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns ReduceResult(initState, NoEffect)
        }

        reducer.testReduceNoOp(initState, inputAction)

        verify {
            reducer.reduce(initState, inputAction)
        }
    }

    @Test
    fun `testReduceNoOp fails when some effects are returned`() = runTest {
        val reducer = mockk<Reducer<TestState, TestAction>> {
            every { reduce(any(), any()) } returns ReduceResult(initState, returnedEffects)
        }

        shouldThrow<AssertionError> {
            reducer.testReduceNoOp(initState, inputAction)
        }
    }

    @Test
    fun `testReduceNoOp fails when state has been changed`() = runTest {
        val reducer =
            Reducer<TestState, TestAction> { _, _ -> ReduceResult(TestState("Changed"), NoEffect) }

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

        coVerify {
            reducer.reduce(initState, inputAction)
        }
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
            every { reduce(any(), any()) } returns ReduceResult(initState, NoEffect)
        }

        shouldThrow<AssertionError> {
            reducer.testReduceException(initState, inputAction, IllegalStateException::class)
        }
    }
}
