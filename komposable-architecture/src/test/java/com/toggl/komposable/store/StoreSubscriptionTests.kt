package com.toggl.komposable.store

import com.toggl.komposable.common.StoreCoroutineTest
import com.toggl.komposable.common.TestAction
import io.mockk.Ordering
import io.mockk.coVerify
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StoreSubscriptionTests : StoreCoroutineTest() {

    @Test
    fun `subscription's method subscribe is called on store creation`() = runTest {
        coVerify(exactly = 1) {
            testSubscription.subscribe(any())
        }
    }

    @Test
    fun `all actions coming from a subscription are reduced in correct order`() = runTest {
        testSubscription.stateFlow.value = TestAction.DoNothingAction
        runCurrent()
        testSubscription.stateFlow.value = TestAction.DoNothingFromEffectAction
        runCurrent()

        coVerify(ordering = Ordering.SEQUENCE) {
            testReducer.reduce(any(), TestAction.DoNothingAction)
            testReducer.reduce(any(), TestAction.DoNothingFromEffectAction)
        }
    }
}
