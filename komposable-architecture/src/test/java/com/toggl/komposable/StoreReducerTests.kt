package com.toggl.komposable

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
            )
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
    fun `all actions are reduced before any effect gets executed`() = runTest {

        val uselessEffect = spyk(TestEffect(TestAction.DoNothingFromEffectAction))
        val clearPropertyEffect = spyk(TestEffect(TestAction.ClearTestPropertyFromEffect))
        val startUselessEffectAction = TestAction.StartEffectAction(uselessEffect)
        val startClearPropertyEffectAction = TestAction.StartEffectAction(clearPropertyEffect)

        val changeTestPropertyAction = TestAction.ChangeTestProperty("123")
        val addTestPropertyAction = TestAction.AddToTestProperty("4")

        testStore.send(
            listOf(
                TestAction.DoNothingAction,
                changeTestPropertyAction,
                addTestPropertyAction,
                startUselessEffectAction,
                startClearPropertyEffectAction,
                TestAction.DoNothingAction,
            )
        )

        runCurrent()

        coVerify(ordering = Ordering.SEQUENCE) {

            // first: reduce sent actions
            testReducer.reduce(any(), TestAction.DoNothingAction)
            testReducer.reduce(any(), changeTestPropertyAction)
            testReducer.reduce(any(), addTestPropertyAction)
            testReducer.reduce(any(), startUselessEffectAction)
            testReducer.reduce(any(), startClearPropertyEffectAction)
            testReducer.reduce(any(), TestAction.DoNothingAction)

            // second: execute effects
            uselessEffect.execute()
            clearPropertyEffect.execute()

            // third: reduce action returned from testEffect1
            testReducer.reduce(any(), TestAction.DoNothingFromEffectAction)
            testReducer.reduce(any(), TestAction.ClearTestPropertyFromEffect)
        }
    }
}
