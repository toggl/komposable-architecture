package com.toggl.komposable.sample.todos.title

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog

@Composable
fun Title(title: String, isEdited: Boolean, onEditTapped: () -> Unit, onEditFinished: () -> Unit, onTitleChanged: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title)
        IconButton(onClick = onEditTapped) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit title")
        }
        if (isEdited) {
            var editedTitle by remember { mutableStateOf(title) }
            Dialog(onDismissRequest = onEditFinished) {
                TextField(
                    value = editedTitle,
                    onValueChange = {
                        editedTitle = it
                        onTitleChanged(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
