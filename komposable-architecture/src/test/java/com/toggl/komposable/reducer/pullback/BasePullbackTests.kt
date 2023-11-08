package com.toggl.komposable.reducer.pullback

import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.common.LocalTestAction
import com.toggl.komposable.common.LocalTestState
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestState
import com.toggl.komposable.extensions.effectOf
import com.toggl.komposable.test.testReduce
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

abstract class BasePullbackTests {
    abstract val localReducer: Reducer<LocalTestState, LocalTestAction>
    abstract val pulledBackReducer: Reducer<TestState, TestAction>

    @Test
    fun `local reducer should be called when mapToLocalAction returns actions`() = runTest {
        val globalState = TestState("", 1)
        val action = TestAction.LocalActionWrapper(LocalTestAction.ChangeTestIntProperty(2))

        pulledBackReducer.testReduce(
            globalState,
            action,
        ) { state, effect ->
            state shouldBe globalState.copy(testIntProperty = 2)
            effect shouldBe NoEffect
        }
        verify {
            localReducer.reduce(
                LocalTestState(1),
                action.action,
            )
        }
    }

    @Test
    fun `local reducer effect results should be correctly wrapped`() = runTest {
        val globalState = TestState("", 1)
        val action = TestAction.LocalActionWrapper(
            LocalTestAction.StartEffectAction(effectOf(LocalTestAction.DoNothingFromEffectAction)),
        )

        pulledBackReducer.testReduce(
            globalState,
            action,
        ) { state, effect ->
            state shouldBe globalState
            effect.actions() shouldBe flowOf(TestAction.LocalActionWrapper(LocalTestAction.DoNothingFromEffectAction))
        }
        verify {
            localReducer.reduce(
                LocalTestState(1),
                action.action,
            )
        }
    }

    @Test
    fun `local reducer should not be called when mapToLocalAction returns null`() = runTest {
        val globalState = TestState("", 1)
        val action = TestAction.ChangeTestProperty("")

        pulledBackReducer.testReduce(
            globalState,
            action,
        ) { state, effect ->
            state shouldBe globalState
            effect shouldBe NoEffect
        }
        verify {
            localReducer wasNot Called
        }
    }
}
