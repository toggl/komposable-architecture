package com.toggl.komposable.sample.todo.di

import android.app.Application
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.extensions.combine
import com.toggl.komposable.extensions.createStore
import com.toggl.komposable.extensions.pullback
import com.toggl.komposable.sample.todo.domain.AppAction
import com.toggl.komposable.sample.todo.domain.AppState
import com.toggl.komposable.sample.todo.domain.unwrap
import com.toggl.komposable.sample.todo.edit.domain.EditAction
import com.toggl.komposable.sample.todo.edit.domain.EditReducer
import com.toggl.komposable.sample.todo.edit.domain.EditState
import com.toggl.komposable.sample.todo.list.domain.ListAction
import com.toggl.komposable.sample.todo.list.domain.ListReducer
import com.toggl.komposable.sample.todo.list.domain.ListState
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.scope.StoreScopeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TodoModule {

    @Provides
    fun appReducer(
        listReducer: ListReducer,
        editReducer: EditReducer
    ): Reducer<AppState, AppAction> =
        combine(
            listReducer.pullback(
                mapToLocalState = { appState -> ListState(appState.todoList) },
                mapToLocalAction = AppAction::unwrap,
                mapToGlobalState = { appState, listState -> appState.copy(todoList = listState.todoList) },
                mapToGlobalAction = { listAction -> AppAction.List(listAction) }
            ),
            editReducer.pullback(
                mapToLocalState = { appState -> EditState(appState.editableTodo) },
                mapToLocalAction = AppAction::unwrap,
                mapToGlobalState = { appState, listState -> appState.copy(editableTodo = listState.editableTodo) },
                mapToGlobalAction = { listAction -> AppAction.Edit(listAction) }
            )
        )

    @Provides
    @Singleton
    fun appStore(
        reducer: Reducer<AppState, AppAction>,
        dispatcherProvider: DispatcherProvider,
        application: Application
    ): Store<AppState, AppAction> {
        return createStore(
            initialState = AppState(),
            reducer = reducer,
            dispatcherProvider = dispatcherProvider,
            storeScopeProvider = application as StoreScopeProvider
        )
    }

    @Provides
    @Singleton
    fun dispatcherProvider(): DispatcherProvider =
        DispatcherProvider(
            io = Dispatchers.IO,
            computation = Dispatchers.Default,
            main = Dispatchers.Main
        )
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object AppViewModelModule {

    @Provides
    fun listStore(store: Store<AppState, AppAction>): Store<ListState, ListAction> =
        store.view(
            mapToLocalState = { appState -> ListState(appState.todoList) },
            mapToGlobalAction = { listAction -> AppAction.List(listAction) }
        )

    @Provides
    fun editStore(store: Store<AppState, AppAction>): Store<EditState, EditAction> =
        store.view(
            mapToLocalState = { appState -> EditState(appState.editableTodo) },
            mapToGlobalAction = { editAction -> AppAction.Edit(editAction) }
        )
}
