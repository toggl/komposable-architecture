package com.toggl.komposable.sample.todo.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.toggl.komposable.extensions.dispatch
import com.toggl.komposable.sample.todo.TodoItem

@Composable
fun ListPage() {
    Text("List Page")
    TodoList(listOf())
}

@Composable
private fun TodoList(todoList: List<TodoItem>) {
    LazyColumn {
        items(
            items = todoList,
            itemContent = { TodoItem(it) }
        )
    }
}

@Composable
private fun TodoItem(todo: TodoItem) {
    Column {
        Text(text = todo.title)
        Text(text = todo.description)
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
