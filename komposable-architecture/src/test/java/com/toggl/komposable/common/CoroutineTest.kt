package com.toggl.komposable.common

import com.toggl.komposable.scope.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class CoroutineTest {
    private val testDispatcher = TestCoroutineDispatcher()
    val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, Dispatchers.Main)
    val testCoroutineScope = TestCoroutineScope(testDispatcher)

    @BeforeEach
    open fun beforeTest() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    open fun afterTest() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}
