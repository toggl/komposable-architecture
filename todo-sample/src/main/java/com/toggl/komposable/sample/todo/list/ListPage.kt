package com.toggl.komposable.sample.todo.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toggl.komposable.sample.todo.collectViewStateWhenStarted
import com.toggl.komposable.sample.todo.data.TodoItem

@Composable
fun ListPage() {
    val listViewState by hiltViewModel<ListStoreViewModel>().collectViewStateWhenStarted()
    TodoList(listViewState.todoList)
}

@Composable
private fun TodoList(todoList: List<TodoItem>) {
    if (todoList.isNotEmpty()) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(
                items = todoList,
                itemContent = { TodoItem(it) }
            )
        }
    } else {
        Text(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            text = "Add your first Todo!",
            style = MaterialTheme.typography.h3
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
        onClick = { listStore.send(ListAction.AddTodoTapped) }
    ) {
        Icon(Icons.Rounded.Add, contentDescription = "Add New Todo Item")
    }
}
