package com.toggl.komposable

import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Subscription
import com.toggl.komposable.common.CoroutineTest
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestState
import com.toggl.komposable.common.TestSubscription
import com.toggl.komposable.common.createTestStore
import io.mockk.Ordering
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test

class StoreSubscriptionTests : CoroutineTest() {

    @Test
    fun `subscription's method subscribe is called on store creation`() = runBlockingTest {
        val testSubscription = mockk<Subscription<TestState, TestAction>>(relaxed = true)
        createTestStore(subscription = testSubscription)

        coVerify(exactly = 1) {
            testSubscription.subscribe(any())
        }
    }

    @Test
    fun `all actions coming from a subscription are reduced in correct order`() = runBlockingTest {
        val testReducer = mockk<Reducer<TestState, TestAction>>(relaxed = true)
        val testSubscription = TestSubscription()
        createTestStore(reducer = testReducer, subscription = testSubscription)

        testSubscription.stateFlow.value = TestAction.DoNothingAction
        testSubscription.stateFlow.value = TestAction.DoNothingFromEffectAction

        coVerify(ordering = Ordering.SEQUENCE) {
            testReducer.reduce(any(), TestAction.DoNothingAction)
            testReducer.reduce(any(), TestAction.DoNothingFromEffectAction)
        }
    }
}
