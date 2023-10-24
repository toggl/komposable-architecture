package com.toggl.komposable.reducer

import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestState
import com.toggl.komposable.extensions.combine
import com.toggl.komposable.extensions.effectOf
import com.toggl.komposable.test.testReduce
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
    private val effectsFromFirst = effectOf(TestAction.DoNothingAction)
    private val firstReducer: Reducer<TestState, TestAction> = mockk<Reducer<TestState, TestAction>> {
        every { reduce(originalState, any()) } returns ReduceResult(stateFromFirst, effectsFromFirst)
    }

    private val stateFromSecond = TestState("second")
    private val effectsFromSecond = effectOf(TestAction.DoNothingFromEffectAction)
    private val secondReducer: Reducer<TestState, TestAction> = mockk<Reducer<TestState, TestAction>> {
        every { reduce(stateFromFirst, any()) } returns ReduceResult(stateFromSecond, effectsFromSecond)
    }

    private val combinedReducer = combine(firstReducer, secondReducer)

    @Test
    fun `reducers should be called sequentially`() = runTest {
        combinedReducer.testReduce(originalState, TestAction.DoNothingAction) { state, effects ->
            state shouldBe stateFromSecond
            effects shouldBe effectsFromFirst + effectsFromSecond
        }

        verify(ordering = Ordering.SEQUENCE) {
            firstReducer.reduce(originalState, TestAction.DoNothingAction)
            secondReducer.reduce(stateFromFirst, TestAction.DoNothingAction)
        }
    }
}
