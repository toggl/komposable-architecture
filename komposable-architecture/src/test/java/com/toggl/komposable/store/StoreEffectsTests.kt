package com.toggl.komposable.store

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.common.StoreCoroutineTest
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestEffect
import io.mockk.Ordering
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StoreEffectsTests : StoreCoroutineTest() {
    @Test
    fun `multiple effects can continue sending actions simultaneously`() = runTest {
        val effect1Flow = MutableStateFlow(TestAction.ChangeTestProperty("0"))
        val effect1 = Effect {
            effect1Flow
        }

        val effect2Flow = MutableStateFlow(TestAction.ChangeTestProperty("10"))
        val effect2 = Effect {
            effect2Flow
        }

        testStore.send(TestAction.StartEffectAction(effect1))
        runCurrent()
        effect1Flow.value = TestAction.ChangeTestProperty("1")
        runCurrent()
        effect1Flow.value = TestAction.ChangeTestProperty("2")
        runCurrent()
        testStore.send(TestAction.StartEffectAction(effect2))
        runCurrent()
        effect2Flow.value = TestAction.ChangeTestProperty("11")
        runCurrent()
        effect1Flow.value = TestAction.ChangeTestProperty("3")
        runCurrent()
        effect2Flow.value = TestAction.ChangeTestProperty("12")
        runCurrent()

        coVerify(ordering = Ordering.SEQUENCE) {
            testReducer.reduce(any(), TestAction.StartEffectAction(effect1))
            testReducer.reduce(any(), TestAction.ChangeTestProperty("0"))
            testReducer.reduce(any(), TestAction.ChangeTestProperty("1"))
            testReducer.reduce(any(), TestAction.ChangeTestProperty("2"))
            testReducer.reduce(any(), TestAction.StartEffectAction(effect2))
            testReducer.reduce(any(), TestAction.ChangeTestProperty("10"))
            testReducer.reduce(any(), TestAction.ChangeTestProperty("11"))
            testReducer.reduce(any(), TestAction.ChangeTestProperty("3"))
            testReducer.reduce(any(), TestAction.ChangeTestProperty("12"))
        }
    }

    @Test
    fun `all actions are reduced before any effect gets processed`() = runTest {
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
            ),
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
            uselessEffect.actions()
            clearPropertyEffect.actions()

            // third: reduce action returned from testEffect1
            testReducer.reduce(any(), TestAction.ClearTestPropertyFromEffect)
            testReducer.reduce(any(), TestAction.DoNothingFromEffectAction)
        }
    }
}
