package com.toggl.komposable.sample.todo.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toggl.komposable.sample.todo.collectViewStateWhenStarted

@Composable
fun EditPage() {
    val store = hiltViewModel<EditStoreViewModel>()
    val editViewState by store.collectViewStateWhenStarted()
    val editableTodoItem = editViewState.editableTodo
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .fillMaxSize(),
    ) {
        Text(
            text = "Edit Page",
            style = MaterialTheme.typography.h3
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            value = editableTodoItem.title,
            onValueChange = { store.send(EditAction.TitleChanged(it)) },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f),
            label = { Text("Description") },
            value = editableTodoItem.description,
            onValueChange = { store.send(EditAction.DescriptionChanged(it)) },
            maxLines = 20
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
