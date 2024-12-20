package com.toggl.komposable.sample.todos

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toggl.komposable.architecture.ChildStates
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.WrapperAction
import com.toggl.komposable.extensions.debounce
import com.toggl.komposable.extensions.named
import com.toggl.komposable.extensions.withEffect
import com.toggl.komposable.extensions.withoutEffect
import com.toggl.komposable.sample.todos.title.TitleAction
import com.toggl.komposable.sample.todos.title.TitleState
import com.toggl.komposable.sample.todos.todo.Todo
import com.toggl.komposable.sample.todos.todo.TodoAction
import com.toggl.komposable.sample.todos.todo.TodoState
import java.util.UUID

sealed class TodosAction {
    data object AddTodoButtonTapped : TodosAction()
    data class FilterChanged(val filter: Filter) : TodosAction()
    data object ClearCompletedButtonTapped : TodosAction()
    data object SortCompletedTodos : TodosAction()
    data class Delete(val ids: Set<UUID>) : TodosAction()
    data class Todo(val index: Int, val action: TodoAction) : TodosAction()

    @WrapperAction
    data class Title(val action: TitleAction) : TodosAction()
}

enum class Filter {
    All,
    Active,
    Completed,
}

@ChildStates(TitleState::class)
data class TodosState(
    val title: String = "Todos",
    val isTitleEdited: Boolean = false,
    val todos: List<TodoState> = emptyList(),
    val filter: Filter = Filter.All,
) {
    val filteredTodos: List<TodoState> = when (filter) {
        Filter.All -> todos
        Filter.Active -> todos.filter { !it.isComplete }
        Filter.Completed -> todos.filter { it.isComplete }
    }
}

class TodosReducer : Reducer<TodosState, TodosAction> {
    override fun reduce(state: TodosState, action: TodosAction): ReduceResult<TodosState, TodosAction> =
        when (action) {
            TodosAction.AddTodoButtonTapped ->
                state.copy(todos = state.todos + TodoState(UUID.randomUUID(), "", false)).withoutEffect()
            is TodosAction.FilterChanged ->
                state.copy(filter = action.filter).withoutEffect()
            TodosAction.ClearCompletedButtonTapped ->
                state.copy(todos = state.todos.filter { !it.isComplete }).withoutEffect()
            is TodosAction.Delete ->
                state.copy(todos = state.todos.filterNot { action.ids.contains(it.id) }).withoutEffect()
            TodosAction.SortCompletedTodos ->
                state.copy(todos = state.todos.sortedBy { it.isComplete }).withoutEffect()
            is TodosAction.Todo -> if (action.action is TodoAction.IsCompleteChanged) {
                state.withEffect { of(TodosAction.SortCompletedTodos).debounce("sort", 1000).named("sort") }
            } else {
                state.withoutEffect()
            }
            is TodosAction.Title -> state.withEffect()
        }
}

@Composable
fun TodoList(todosState: TodosState, onCheckedChange: (Int, Boolean) -> Unit, onDescriptionChanged: (Int, String) -> Unit) {
    LazyColumn(modifier = Modifier.padding(start = 6.dp)) {
        for ((index, todo) in todosState.filteredTodos.withIndex()) {
            item(key = todo.id) {
                Todo(
                    todo = todo,
                    modifier = Modifier.animateItem(),
                    onCheckedChange = { onCheckedChange(index, it) },
                    onDescriptionChanged = { onDescriptionChanged(index, it) },
                )
            }
        }
    }
}
