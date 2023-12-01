package com.toggl.komposable.sample.todos

import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.test.testReduce
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

private val reducer = TodoReducer()
private val initialState = TodoState(UUID.randomUUID(), "description", false)

@DisplayName("IsCompleteChanged action should")
class TodoTests {
    @Test
    fun `change the isComplete accordingly and return no effects`() = runTest {
        reducer.testReduce(
            initialState = initialState,
            action = TodoAction.IsCompleteChanged(true),
        ) { state, effect ->
            state shouldBe initialState.copy(isComplete = true)
            effect shouldBe NoEffect
        }
    }
}

@DisplayName("DescriptionChanged action should")
class DescriptionChangedTests {
    @Test
    fun `change the description accordingly and return no effects`() = runTest {
        reducer.testReduce(
            initialState = initialState,
            action = TodoAction.DescriptionChanged("new description"),
        ) { state, effect ->
            state shouldBe initialState.copy(description = "new description")
            effect shouldBe NoEffect
        }
    }
}
