package com.toggl.komposable.sample.todo.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toggl.komposable.sample.todo.collectAsStateWhenStarted
import com.toggl.komposable.sample.todo.data.TodoItem
import kotlinx.coroutines.flow.map

@Composable
fun ListPage() {
    val list = hiltViewModel<ListStoreViewModel>().state
        .map { it.todoList }
        .collectAsStateWhenStarted(initial = emptyList())
        .value
    TodoList(list)
}

@Composable
private fun TodoList(todoList: List<TodoItem>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(
            items = todoList,
            itemContent = { TodoItem(it) }
        )
    }
}

@Composable
private fun TodoItem(todo: TodoItem) {
    Column {
        Text(text = todo.title, style = MaterialTheme.typography.h6)
        Text(text = todo.description, style = MaterialTheme.typography.body1)
    }
}

@Composable
fun AddTodoFab() {
    val listStore = hiltViewModel<ListStoreViewModel>()
    FloatingActionButton(
        onClick = { listStore.dispatch(ListAction.AddTodoTapped) }
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add New Todo Item")
    }
}
