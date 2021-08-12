package com.toggl.komposable

import com.toggl.komposable.common.StoreCoroutineTest
import com.toggl.komposable.common.TestAction
import io.mockk.Ordering
import io.mockk.coVerify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test

class StoreSubscriptionTests : StoreCoroutineTest() {

    @Test
    fun `subscription's method subscribe is called on store creation`() = runBlockingTest {
        coVerify(exactly = 1) {
            testSubscription.subscribe(any())
        }
    }

    @Test
    fun `all actions coming from a subscription are reduced in correct order`() = runBlockingTest {
        testSubscription.stateFlow.value = TestAction.DoNothingAction
        testSubscription.stateFlow.value = TestAction.DoNothingFromEffectAction

        coVerify(ordering = Ordering.SEQUENCE) {
            testReducer.reduce(any(), TestAction.DoNothingAction)
            testReducer.reduce(any(), TestAction.DoNothingFromEffectAction)
        }
    }
}
