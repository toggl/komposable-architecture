package com.toggl.komposable.common

import com.toggl.komposable.architecture.Store
import com.toggl.komposable.exceptions.ExceptionHandler
import com.toggl.komposable.scope.DispatcherProvider
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class StoreCoroutineTest {
    private val testDispatcher = StandardTestDispatcher()
    val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
    val testCoroutineScope = TestScope(testDispatcher)
    lateinit var testStore: Store<TestState, TestAction>
    lateinit var testReducer: TestReducer
    lateinit var testSubscription: TestSubscription
    lateinit var testExceptionHandler: ExceptionHandler

    @BeforeEach
    open fun beforeTest() {
        Dispatchers.setMain(testDispatcher)
        testReducer = spyk(TestReducer())
        testSubscription = spyk(TestSubscription())
        testExceptionHandler = spyk(TestStoreExceptionHandler())
        testStore = spyk(
            createTestStore(
                reducer = testReducer,
                subscription = testSubscription,
                defaultExceptionHandler = testExceptionHandler,
            ),
        )
    }

    @AfterEach
    open fun afterTest() {
        Dispatchers.resetMain()
        testDispatcher.scheduler.runCurrent()
    }
}
