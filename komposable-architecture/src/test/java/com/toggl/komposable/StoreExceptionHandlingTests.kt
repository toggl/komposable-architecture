package com.toggl.komposable

import com.toggl.komposable.common.CoroutineTest
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestException
import com.toggl.komposable.common.TestSubscription
import com.toggl.komposable.common.createTestStore
import com.toggl.komposable.exceptions.ExceptionHandler
import com.toggl.komposable.extensions.dispatch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StoreExceptionHandlingTests : CoroutineTest() {

    @Test
    fun `reduce exception should be handled`() = runBlockingTest {
        val defaultExceptionHandler = mockk<ExceptionHandler>(relaxed = true)
        val store = createTestStore(defaultExceptionHandler = defaultExceptionHandler)

        store.dispatch(TestAction.ThrowExceptionAction)

        coVerify(exactly = 1) { defaultExceptionHandler.handleException(TestException) }
    }

    @Test
    fun `effect exception should be handled`() = runBlockingTest {
        val defaultExceptionHandler = mockk<ExceptionHandler>(relaxed = true)
        val store = createTestStore(defaultExceptionHandler = defaultExceptionHandler)
        store.dispatch(TestAction.StartExceptionThrowingEffectAction)

        coVerify(exactly = 1) { defaultExceptionHandler.handleException(TestException) }
    }

    @Test
    fun `subscription exception should be handled`() = runBlockingTest {
        val defaultExceptionHandler = mockk<ExceptionHandler>(relaxed = true)
        val subscription = mockk<TestSubscription> {
            coEvery { subscribe(any()) } throws TestException
        }

        assertThrows<Throwable> {
            createTestStore(
                subscription = subscription,
                defaultExceptionHandler = defaultExceptionHandler
            )
        }

        coVerify(exactly = 1) { defaultExceptionHandler.handleException(TestException) }
    }
}
