package com.toggl.komposable.sample.todo

import com.toggl.komposable.extensions.noEffect
import com.toggl.komposable.sample.todo.common.CoroutineTest
import com.toggl.komposable.sample.todo.data.TodoItem
import com.toggl.komposable.sample.todo.list.ListAction
import com.toggl.komposable.sample.todo.list.ListReducer
import com.toggl.komposable.sample.todo.list.ListState
import com.toggl.komposable.test.testReduce
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TestListReducer : CoroutineTest() {
    private val reducer = ListReducer()
    private val testTodoItem = TodoItem(1, "title", "description")

    @Test
    fun `ListUpdated action should update the list of todos and return no effects`() = runTest {
        val initialState = ListState(todoList = emptyList(), backStack = emptyList())
        reducer.testReduce(
            initialState,
            ListAction.ListUpdated(listOf(testTodoItem))
        ) { state, effects ->
            assertEquals(initialState.copy(todoList = listOf(testTodoItem)), state)
            assertEquals(noEffect(), effects)
        }
    }

    @Test
    fun `AddTodoTapped action should add new destination to the backstack and return no effects`() = runTest {
        val initialState = ListState(todoList = emptyList(), backStack = emptyList())
        reducer.testReduce(
            initialState,
            ListAction.AddTodoTapped
        ) { state, effects ->
            assertEquals(initialState.copy(backStack = listOf(AppDestination.Add)), state)
            assertEquals(noEffect(), effects)
        }
    }
}
