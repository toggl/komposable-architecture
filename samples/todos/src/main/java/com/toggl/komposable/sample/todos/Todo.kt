package com.toggl.komposable.sample.todos

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
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.withoutEffect
import java.util.UUID

sealed class TodoAction {
    data class IsCompleteChanged(val isComplete: Boolean) : TodoAction()
    data class DescriptionChanged(val description: String) : TodoAction()
}

data class TodoState(
    val id: UUID,
    val description: String,
    val isComplete: Boolean,
)

class TodoReducer : Reducer<TodoState, TodoAction> {
    override fun reduce(state: TodoState, action: TodoAction): ReduceResult<TodoState, TodoAction> =
        when (action) {
            is TodoAction.DescriptionChanged -> state.copy(description = action.description).withoutEffect()
            is TodoAction.IsCompleteChanged -> state.copy(isComplete = action.isComplete).withoutEffect()
        }
}

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
