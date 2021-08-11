package com.toggl.komposable

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.common.CoroutineTest
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestState
import com.toggl.komposable.common.createTestStore
import com.toggl.komposable.extensions.dispatch
import com.toggl.komposable.extensions.effectOf
import com.toggl.komposable.extensions.noEffect
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test

class StoreReducerTests : CoroutineTest() {

    @Test
    fun `reducer shouldn't be called if the list of dispatched actions is empty`() = runBlockingTest {
        val testReducer = mockk<Reducer<TestState, TestAction>>(relaxed = true)
        val store = createTestStore(reducer = testReducer)

        store.dispatch(emptyList())

        coVerify(exactly = 0) { testReducer.reduce(any(), any()) }
    }

    @Test
    fun `reducer should be called exactly once if one actions is dispatched`() = runBlockingTest {
        val testReducer = mockk<Reducer<TestState, TestAction>>(relaxed = true)
        val store = createTestStore(reducer = testReducer)

        store.dispatch(TestAction.DoNothingAction)

        coVerify(exactly = 1) { testReducer.reduce(any(), TestAction.DoNothingAction) }
    }

    @Test
    fun `reducer should be called for each action dispatched in order in which they were provided`() = runBlockingTest {
        val testReducer = mockk<Reducer<TestState, TestAction>>(relaxed = true)
        val store = createTestStore(reducer = testReducer)

        store.dispatch(
            listOf(
                TestAction.DoNothingAction,
                TestAction.StartEffectAction,
                TestAction.DoNothingFromEffectAction,
            )
        )

        coVerify(ordering = Ordering.SEQUENCE) {
            testReducer.reduce(any(), TestAction.DoNothingAction)
            testReducer.reduce(any(), TestAction.StartEffectAction)
            testReducer.reduce(any(), TestAction.DoNothingFromEffectAction)
        }
    }

    @Test
    fun `all actions are reduced before any effect gets executed`() = runBlockingTest {
        val testEffect1 = mockk<Effect<TestAction>> {
            coEvery { execute() } returns TestAction.DoNothingAction
        }
        val testEffect2 = mockk<Effect<TestAction>> {
            coEvery { execute() } returns null
        }

        val testReducer = mockk<Reducer<TestState, TestAction>>(relaxed = true) {
            coEvery { reduce(any(), TestAction.StartEffectAction) } returns effectOf(testEffect1)
            coEvery { reduce(any(), TestAction.DoNothingAction) } returns noEffect()
            coEvery { reduce(any(), TestAction.DoNothingFromEffectAction) } returns effectOf(testEffect2)
        }

        val store = createTestStore(reducer = testReducer)

        store.dispatch(
            listOf(
                TestAction.StartEffectAction,
                TestAction.DoNothingAction,
                TestAction.DoNothingFromEffectAction,
            )
        )

        coVerify(ordering = Ordering.SEQUENCE) {
            // first: reduce dispatched actions
            testReducer.reduce(any(), TestAction.StartEffectAction)
            testReducer.reduce(any(), TestAction.DoNothingAction)
            testReducer.reduce(any(), TestAction.DoNothingFromEffectAction)

            // second: execute effects
            testEffect1.execute()
            testEffect2.execute()

            // third: reduce action returned from testEffect1
            testReducer.reduce(any(), TestAction.DoNothingAction)
        }
    }
}
