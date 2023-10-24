package com.toggl.komposable.store

import com.toggl.komposable.common.StoreCoroutineTest
import com.toggl.komposable.common.TestAction
import com.toggl.komposable.common.TestException
import com.toggl.komposable.common.TestSubscription
import com.toggl.komposable.common.createTestStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StoreExceptionHandlingTests : StoreCoroutineTest() {

    @Test
    fun `reduce exception should be handled`() = runTest {
        testStore.send(TestAction.ThrowExceptionAction)
        runCurrent()
        coVerify(exactly = 1) { testExceptionHandler.handleException(TestException) }
    }

    @Test
    fun `effect exception should be handled`() = runTest {
        testStore.send(TestAction.StartExceptionThrowingEffectAction)
        runCurrent()
        coVerify(exactly = 1) { testExceptionHandler.handleException(TestException) }
    }

    @Test
    fun `subscription exception should be handled`() = runTest {
        val subscription = mockk<TestSubscription> {
            coEvery { subscribe(any()) } throws TestException
        }

        runCurrent()

        assertThrows<Throwable> {
            createTestStore(
                subscription = subscription,
                defaultExceptionHandler = testExceptionHandler,
            )
        }

        coVerify(exactly = 1) { testExceptionHandler.handleException(TestException) }
    }
}
