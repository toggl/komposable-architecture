package com.toggl.komposable.sample.todos

import com.toggl.komposable.extensions.forEachList
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.test.store.createTestStore
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class TodosTests {
    private val testDispatcher = StandardTestDispatcher()
    val dispatcherProvider = DispatcherProvider(testDispatcher, testDispatcher, testDispatcher)
    val testCoroutineScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @Nested
    @DisplayName("TodosAction.AddTodoButtonTapped action should")
    inner class AddTodoButtonTappedTests {
        private val store = createTestStore(
            initialState = { TodosState() },
            reducer = { TodosReducer() },
            dispatcherProvider = dispatcherProvider,
            testCoroutineScope = testCoroutineScope,
        )

        @Test
        fun `add items`() = runTest {
            mockkStatic(UUID::class)
            every {
                UUID.randomUUID()
            }.returnsMany(UUID(0, 1), UUID(0, 2))

            store.send(TodosAction.AddTodoButtonTapped) {
                it.copy(todos = it.todos + TodoState(UUID(0, 1), "", false))
            }

            store.send(TodosAction.AddTodoButtonTapped) {
                it.copy(todos = it.todos + TodoState(UUID(0, 2), "", false))
            }
        }
    }

    @Nested
    @DisplayName("TodoAction.DescriptionChanged action should")
    inner class DescriptionChangedTests {
        private val store = createTestStore(
            initialState = {
                TodosState(
                    todos = listOf(
                        TodoState(UUID(0, 1), "", false),
                        TodoState(UUID(0, 2), "something", false),
                    ),
                )
            },
            reducer = {
                TodosReducer().forEachList(
                    elementReducer = TodoReducer(),
                    mapToElementAction = { action -> if (action is TodosAction.Todo) action.index to action.action else null },
                    mapToElementList = { state -> state.todos },
                    mapToParentAction = { action, index -> TodosAction.Todo(index, action) },
                    mapToParentState = { state, todosMap -> state.copy(todos = todosMap) },
                )
            },
            dispatcherProvider = dispatcherProvider,
            testCoroutineScope = testCoroutineScope,
        )

        @Test
        fun `edit descriptions`() = runTest {
            store.send(TodosAction.Todo(0, TodoAction.DescriptionChanged("edited"))) {
                it.copy(
                    todos = listOf(
                        TodoState(UUID(0, 1), "edited", false),
                        TodoState(UUID(0, 2), "something", false),
                    ),
                )
            }
            store.send(TodosAction.Todo(1, TodoAction.DescriptionChanged("something else"))) {
                it.copy(
                    todos = listOf(
                        TodoState(UUID(0, 1), "edited", false),
                        TodoState(UUID(0, 2), "something else", false),
                    ),
                )
            }
        }
    }

    @Nested
    @DisplayName("TodoAction.IsCompleteChanged action should")
    inner class IsCompleteChangedTests {
        private val store = createTestStore(
            initialState = {
                TodosState(
                    todos = listOf(
                        TodoState(UUID(0, 1), "xx", false),
                        TodoState(UUID(0, 2), "aa", false),
                    ),
                )
            },
            reducer = {
                TodosReducer().forEachList(
                    elementReducer = TodoReducer(),
                    mapToElementAction = { action -> if (action is TodosAction.Todo) action.index to action.action else null },
                    mapToElementList = { state -> state.todos },
                    mapToParentAction = { action, index -> TodosAction.Todo(index, action) },
                    mapToParentState = { state, todosMap -> state.copy(todos = todosMap) },
                )
            },
            dispatcherProvider = dispatcherProvider,
            testCoroutineScope = testCoroutineScope,
        )

        @Test
        fun `modify the isCompleted flag`() = runTest {
            store.send(TodosAction.Todo(0, TodoAction.IsCompleteChanged(true))) {
                it.copy(
                    todos = listOf(
                        TodoState(UUID(0, 1), "xx", true),
                        TodoState(UUID(0, 2), "aa", false),
                    ),
                )
            }
            testScheduler.advanceTimeBy(500)
            shouldThrow<AssertionError> {
                // Too early
                store.receive(TodosAction.SortCompletedTodos)
            }
            store.send(TodosAction.Todo(1, TodoAction.IsCompleteChanged(true))) {
                it.copy(
                    todos = listOf(
                        TodoState(UUID(0, 1), "xx", true),
                        TodoState(UUID(0, 2), "aa", true),
                    ),
                )
            }
            store.send(TodosAction.Todo(1, TodoAction.IsCompleteChanged(false))) {
                it.copy(
                    todos = listOf(
                        TodoState(UUID(0, 1), "xx", true),
                        TodoState(UUID(0, 2), "aa", false),
                    ),
                )
            }
            // Effects get overridden by the next action until the last one and emits after 1000ms
            testScheduler.advanceTimeBy(1000)
            store.receive(TodosAction.SortCompletedTodos) {
                it.copy(
                    todos = listOf(
                        TodoState(UUID(0, 2), "aa", false),
                        TodoState(UUID(0, 1), "xx", true),
                    ),
                )
            }
        }
    }

    @Nested
    @DisplayName("TodosAction.ClearCompletedButtonTapped action should")
    inner class ClearCompletedButtonTappedTests {
        private val store = createTestStore(
            initialState = {
                TodosState(
                    todos = listOf(
                        TodoState(UUID(0, 1), "forEach", true),
                        TodoState(UUID(0, 2), "testStore", false),
                    ),
                )
            },
            reducer = { TodosReducer() },
            dispatcherProvider = dispatcherProvider,
            testCoroutineScope = testCoroutineScope,
        )

        @Test
        fun `remove completed todos from state`() = runTest {
            store.send(TodosAction.ClearCompletedButtonTapped) {
                it.copy(
                    todos = listOf(
                        TodoState(UUID(0, 2), "testStore", false),
                    ),
                )
            }
        }
    }

    @Nested
    @DisplayName("TodosAction.Delete action should")
    inner class DeleteTests {
        private val store = createTestStore(
            initialState = {
                TodosState(
                    todos = listOf(
                        TodoState(UUID(0, 1), "top", false),
                        TodoState(UUID(0, 2), "middle 1", false),
                        TodoState(UUID(0, 3), "middle 2", false),
                        TodoState(UUID(0, 4), "bottom", false),
                    ),
                )
            },
            reducer = { TodosReducer() },
            dispatcherProvider = dispatcherProvider,
            testCoroutineScope = testCoroutineScope,
        )

        @Test
        fun `remove todo items with the expected uuids`() = runTest {
            store.send(
                TodosAction.Delete(
                    setOf(UUID(0, 2), UUID(0, 3)),
                ),
            ) {
                it.copy(
                    todos = listOf(it.todos.first(), it.todos.last()),
                )
            }
        }
    }

    @Nested
    @DisplayName("TodosAction.FilterChanged action should")
    inner class FilterChangedTests {
        private val store = createTestStore(
            initialState = {
                TodosState(
                    todos = listOf(
                        TodoState(UUID(0, 1), "top", true),
                        TodoState(UUID(0, 2), "middle 1", false),
                        TodoState(UUID(0, 3), "middle 2", false),
                        TodoState(UUID(0, 4), "bottom", true),
                    ),
                )
            },
            reducer = { TodosReducer() },
            dispatcherProvider = dispatcherProvider,
            testCoroutineScope = testCoroutineScope,
        )

        @Test
        fun `set the filter`() = runTest {
            store.send(
                TodosAction.FilterChanged(Filter.Active),
            ) {
                it.copy(filter = Filter.Active)
            }
            store.send(
                TodosAction.FilterChanged(Filter.Completed),
            ) {
                it.copy(filter = Filter.Completed)
            }
        }
    }
}
