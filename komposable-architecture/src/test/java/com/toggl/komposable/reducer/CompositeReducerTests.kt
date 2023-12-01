package com.toggl.komposable.reducer

import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestState
import com.toggl.komposable.extensions.combine
import com.toggl.komposable.test.testReduceState
import io.kotest.matchers.shouldBe
import io.mockk.Ordering
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CompositeReducerTests {
    private val originalState = TestState("original")
    private val stateFromFirst = TestState("first")
    private val firstReducer: Reducer<TestState, TestAction> = mockk<Reducer<TestState, TestAction>> {
        every { reduce(originalState, any()) } returns ReduceResult(stateFromFirst, NoEffect)
    }

    private val stateFromSecond = TestState("second")
    private val secondReducer: Reducer<TestState, TestAction> = mockk<Reducer<TestState, TestAction>> {
        every { reduce(stateFromFirst, any()) } returns ReduceResult(stateFromSecond, NoEffect)
    }

    private val combinedReducer = combine(firstReducer, secondReducer)

    @Test
    fun `reducers should be called sequentially`() = runTest {
        combinedReducer.testReduceState(originalState, TestAction.DoNothingAction) { state ->
            state shouldBe stateFromSecond
        }

        verify(ordering = Ordering.SEQUENCE) {
            firstReducer.reduce(originalState, TestAction.DoNothingAction)
            secondReducer.reduce(stateFromFirst, TestAction.DoNothingAction)
        }
    }
}
