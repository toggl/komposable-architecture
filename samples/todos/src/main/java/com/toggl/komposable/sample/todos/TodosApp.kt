package com.toggl.komposable.sample.todos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.createStore
import com.toggl.komposable.extensions.forEachList
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.scope.StoreScopeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

val todosAppReducer: Reducer<TodosState, TodosAction> = TodosReducer().forEachList(
    elementReducer = TodoReducer(),
    mapToElementAction = { action -> if (action is TodosAction.Todo) action.index to action.action else null },
    mapToElementList = { state -> state.todos },
    mapToParentAction = { action, index -> TodosAction.Todo(index, action) },
    mapToParentState = { state, todosMap -> state.copy(todos = todosMap) },
)

val dispatcherProvider = DispatcherProvider(
    io = Dispatchers.IO,
    computation = Dispatchers.Default,
    main = Dispatchers.Main,
)
val coroutineScope = object : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = dispatcherProvider.main
}
val storeScopeProvider = StoreScopeProvider { coroutineScope }
val todosStore = createStore(
    initialState = TodosState(),
    reducer = todosAppReducer,
    storeScopeProvider = storeScopeProvider,
    dispatcherProvider = dispatcherProvider,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosApp() {
    val todosState by todosStore.state.collectAsState(initial = TodosState())
    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            MediumTopAppBar(
                title = { Text(text = "Todos") },
                actions = {
                    TextButton(onClick = { todosStore.send(TodosAction.ClearCompletedButtonTapped) }) {
                        Text(text = "Clear Completed")
                    }
                },
            )
            TodoFilterRow(
                selectedFilter = todosState.filter,
                send = { todosStore.send(it) },
            )
            OutlinedCard(modifier = Modifier.padding(12.dp)) {
                TodoList(
                    todosState = todosState,
                    onCheckedChange = { id, isCompleted ->
                        todosStore.send(TodosAction.Todo(id, TodoAction.IsCompleteChanged(isCompleted)))
                    },
                    onDescriptionChanged = { id, description ->
                        todosStore.send(TodosAction.Todo(id, TodoAction.DescriptionChanged(description)))
                    },
                )
            }
        }
        ExtendedFloatingActionButton(
            onClick = { todosStore.send(TodosAction.AddTodoButtonTapped) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
            )
            Text(
                text = "Add Todo",
            )
        }
    }
}

@Composable
fun TodoFilterRow(selectedFilter: Filter, send: (TodosAction) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)) {
        TodoFilterChip(
            text = "All",
            selected = selectedFilter == Filter.All,
            onClick = { send(TodosAction.FilterChanged(Filter.All)) },
        )
        TodoFilterChip(
            text = "Active",
            selected = selectedFilter == Filter.Active,
            onClick = { send(TodosAction.FilterChanged(Filter.Active)) },
        )
        TodoFilterChip(
            text = "Completed",
            selected = selectedFilter == Filter.Completed,
            onClick = { send(TodosAction.FilterChanged(Filter.Completed)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        onClick = onClick,
        label = { Text(text) },
        selected = selected,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Done icon",
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
    )
}
