package com.toggl.komposable.sample.todo.list

import com.toggl.komposable.architecture.Subscription
import com.toggl.komposable.sample.todo.AppAction
import com.toggl.komposable.sample.todo.AppState
import com.toggl.komposable.sample.todo.data.TodoDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListSubscription @Inject constructor(val todoDao: TodoDao) : Subscription<AppState, AppAction> {
    override fun subscribe(state: Flow<AppState>): Flow<AppAction> =
        todoDao.getAll().map { AppAction.List(ListAction.ListUpdated(it)) }
}
