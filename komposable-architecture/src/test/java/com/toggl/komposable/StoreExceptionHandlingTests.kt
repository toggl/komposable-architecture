package com.toggl.komposable

import com.toggl.komposable.common.StoreCoroutineTest
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestException
import com.toggl.komposable.common.TestSubscription
import com.toggl.komposable.common.createTestStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StoreExceptionHandlingTests : StoreCoroutineTest() {

    @Test
    fun `reduce exception should be handled`() = runBlockingTest {
        testStore.dispatch(TestAction.ThrowExceptionAction)
        coVerify(exactly = 1) { testExceptionHandler.handleException(TestException) }
    }

    @Test
    fun `effect exception should be handled`() = runBlockingTest {
        testStore.dispatch(TestAction.StartExceptionThrowingEffectAction)
        coVerify(exactly = 1) { testExceptionHandler.handleException(TestException) }
    }

    @Test
    fun `subscription exception should be handled`() = runBlockingTest {
        val subscription = mockk<TestSubscription> {
            coEvery { subscribe(any()) } throws TestException
        }

        assertThrows<Throwable> {
            createTestStore(
                subscription = subscription,
                defaultExceptionHandler = testExceptionHandler
            )
        }

        coVerify(exactly = 1) { testExceptionHandler.handleException(TestException) }
    }
}
