package com.toggl.komposable.sample.todo.edit

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.effectOf
import com.toggl.komposable.extensions.mutateWithoutEffects
import com.toggl.komposable.sample.todo.data.EditableTodoItem
import com.toggl.komposable.sample.todo.data.TodoDao
import com.toggl.komposable.sample.todo.data.TodoItem
import com.toggl.komposable.scope.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditReducer @Inject constructor(
    private val saveTodoEffectFactory: SaveTodoEffect.Factory
) : Reducer<EditState, EditAction> {
    override fun reduce(state: Mutable<EditState>, action: EditAction): List<Effect<EditAction>> =
        when (action) {
            is EditAction.DescriptionChanged -> state.mutateWithoutEffects { copy(editableTodo = editableTodo.copy(description = action.description)) }
            is EditAction.TitleChanged -> state.mutateWithoutEffects { copy(editableTodo = editableTodo.copy(title = action.title)) }
            EditAction.SaveTapped -> effectOf(saveTodoEffectFactory.create(state().editableTodo))
            EditAction.Saved,
            EditAction.CloseTapped -> state.mutateWithoutEffects {
                copy(editableTodo = EditableTodoItem()).popBackStack()
            }
        }
}

class SaveTodoEffect(
    private val dispatcherProvider: DispatcherProvider,
    private val todoDao: TodoDao,
    private val editableTodoItem: EditableTodoItem,
) : Effect<EditAction.Saved> {
    override suspend fun execute(): EditAction.Saved = withContext(dispatcherProvider.io) {
        if (editableTodoItem.id == null) {
            todoDao.insert(TodoItem(title = editableTodoItem.title, description = editableTodoItem.description))
        } else {
            todoDao.update(TodoItem(id = editableTodoItem.id, title = editableTodoItem.title, description = editableTodoItem.description))
        }
        EditAction.Saved
    }

    @Singleton
    class Factory @Inject constructor(
        private val dispatcherProvider: DispatcherProvider,
        private val todoDao: TodoDao
    ) {
        fun create(editableTodoItem: EditableTodoItem) =
            SaveTodoEffect(dispatcherProvider, todoDao, editableTodoItem)
    }
}
