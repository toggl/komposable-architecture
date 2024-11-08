package com.toggl.komposable.sample.todos.todo

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Todo(todo: TodoState, modifier: Modifier = Modifier, onCheckedChange: (Boolean) -> Unit, onDescriptionChanged: (String) -> Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = todo.isComplete, onCheckedChange = onCheckedChange)
        var description by remember { mutableStateOf(todo.description) }
        TextField(
            value = description,
            onValueChange = {
                description = it
                onDescriptionChanged(it)
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
