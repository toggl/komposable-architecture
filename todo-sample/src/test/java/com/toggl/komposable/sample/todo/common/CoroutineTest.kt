package com.toggl.komposable.sample.todo.common

import com.toggl.komposable.scope.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class CoroutineTest {
    private val testDispatcher = StandardTestDispatcher()
    protected val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, Dispatchers.Main)

    @BeforeEach
    open fun beforeTest() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    open fun afterTest() {
        Dispatchers.resetMain()
        testDispatcher.scheduler.runCurrent()
    }
}
