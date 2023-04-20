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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.toggl.komposable.sample.todo.collectViewStateWhenStarted

@Composable
fun EditPage() {
    val store = hiltViewModel<EditStoreViewModel>()
    val editViewState by store.collectViewStateWhenStarted()
    val editableTodoItem = editViewState.editableTodo
    var title by remember { mutableStateOf(TextFieldValue(editableTodoItem.title)) }
    var description by remember { mutableStateOf(TextFieldValue(editableTodoItem.description)) }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .fillMaxSize(),
    ) {
        Text(
            text = "Edit Page",
            style = MaterialTheme.typography.h3,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            value = title,
            onValueChange = {
                title = it
                store.send(EditAction.TitleChanged(it.text))
            },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f),
            label = { Text("Description") },
            value = description,
            onValueChange = {
                description = it
                store.send(EditAction.DescriptionChanged(it.text))
            },
            maxLines = 20,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
