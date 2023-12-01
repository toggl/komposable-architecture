package com.toggl.komposable.store

import com.toggl.komposable.common.StoreCoroutineTest
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestEffect
import io.mockk.Ordering
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StoreReducerTests : StoreCoroutineTest() {

    @Test
    fun `reducer shouldn't be called if the list of sent actions is empty`() = runTest {
        testStore.send(emptyList())
        runCurrent()
        coVerify(exactly = 0) { testReducer.reduce(any(), any()) }
    }

    @Test
    fun `reducer should be called exactly once if one action is sent`() = runTest {
        testStore.send(TestAction.DoNothingAction)
        runCurrent()
        coVerify(exactly = 1) { testReducer.reduce(any(), TestAction.DoNothingAction) }
    }

    @Test
    fun `reducer should be called for each action sent in order in which they were provided`() = runTest {
        val startUselessEffectAction = TestAction.StartEffectAction(TestEffect(TestAction.DoNothingFromEffectAction))
        testStore.send(
            listOf(
                TestAction.DoNothingAction,
                startUselessEffectAction,
                TestAction.DoNothingAction,
            ),
        )

        runCurrent()

        coVerify(ordering = Ordering.SEQUENCE) {
            testReducer.reduce(any(), TestAction.DoNothingAction)
            testReducer.reduce(any(), startUselessEffectAction)
            testReducer.reduce(any(), TestAction.DoNothingAction)
            testReducer.reduce(any(), TestAction.DoNothingFromEffectAction)
        }
    }

    @Test
    fun `effects with multiple actions are processed correctly`() = runTest {
        val flowEffect = spyk(
            TestEffect(
                TestAction.ClearTestPropertyFromEffect,
                TestAction.ChangeTestProperty("123"),
                TestAction.AddToTestProperty("4"),
            ),
        )
        val startFlowEffectAction = TestAction.StartEffectAction(flowEffect)

        testStore.send(
            listOf(
                TestAction.DoNothingAction,
                startFlowEffectAction,
            ),
        )

        runCurrent()

        coVerify(ordering = Ordering.SEQUENCE) {
            // first: reduce sent actions
            testReducer.reduce(any(), TestAction.DoNothingAction)
            testReducer.reduce(any(), startFlowEffectAction)

            // second: reduce action coming from effect
            testReducer.reduce(any(), TestAction.ClearTestPropertyFromEffect)
            testReducer.reduce(any(), TestAction.ChangeTestProperty("123"))
            testReducer.reduce(any(), TestAction.AddToTestProperty("4"))
        }
    }
}
