package com.toggl.komposable.store

import app.cash.turbine.test
import com.toggl.komposable.common.StoreCoroutineTest
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestEffect
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.ExperimentalTime

class StoreStateTests : StoreCoroutineTest() {

    @ExperimentalTime
    @Test
    fun `state is emitted in batches after all sent actions were reduced`() = runTest {
        testStore.state.test {
            testStore.send(
                listOf(
                    TestAction.DoNothingAction,
                    TestAction.ChangeTestProperty("123"),
                    TestAction.AddToTestProperty("4"),
                    TestAction.DoNothingAction,
                ),
            )

            // initial state
            awaitItem().testProperty shouldBe ""
            // TestAction.DoNothingAction
            // TestAction.ChangeTestProperty("123")
            // TestAction.AddToTestProperty("4")
            // TestAction.DoNothingAction
            awaitItem().testProperty shouldBe "1234"

            cancelAndConsumeRemainingEvents()
        }
    }

    @ExperimentalTime
    @Test
    fun `new state is emitted only when it's different than the previous one`() = runTest {
        testStore.state.test {
            testStore.send(
                listOf(
                    TestAction.DoNothingAction,
                    TestAction.ChangeTestProperty("123"),
                    TestAction.AddToTestProperty("4"),
                    TestAction.ChangeTestProperty(""),
                ),
            )

            // initial state
            awaitItem().testProperty shouldBe ""
            // TestAction.DoNothingAction
            // TestAction.ChangeTestProperty("123")
            // TestAction.ChangeTestProperty("")
            // we got back to the initial state, it doesn't make sense to emit anything

            cancelAndConsumeRemainingEvents()
        }
    }

    @ExperimentalTime
    @Test
    fun `effects emit new state only after the result state of previously sent actions was emitted`() = runTest {
        testStore.state.test {
            testStore.send(
                listOf(
                    TestAction.DoNothingAction,
                    TestAction.ChangeTestProperty("123"),
                    TestAction.AddToTestProperty("4"),
                    TestAction.StartEffectAction(TestEffect(TestAction.DoNothingFromEffectAction)),
                    TestAction.StartEffectAction(TestEffect(TestAction.ClearTestPropertyFromEffect)),
                ),
            )

            // initial state
            awaitItem().testProperty shouldBe ""
            // TestAction.DoNothingAction
            // TestAction.ChangeTestProperty("123")
            // TestAction.AddToTestProperty("4")
            // TestAction.StartEffectAction(TestEffect(TestAction.DoNothingFromEffectAction))
            // TestAction.StartEffectAction(TestEffect(TestAction.ClearTestPropertyFromEffect))
            awaitItem().testProperty shouldBe "1234"
            // From effect.execute() => TestAction.DoNothingFromEffectAction
            // From effect.execute() => TestAction.ClearTestPropertyFromEffect
            awaitItem().testProperty shouldBe ""

            cancelAndConsumeRemainingEvents()
        }
    }
}
