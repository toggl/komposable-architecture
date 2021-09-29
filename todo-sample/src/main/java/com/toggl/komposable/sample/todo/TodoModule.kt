package com.toggl.komposable.sample.todo

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.extensions.combine
import com.toggl.komposable.extensions.createStore
import com.toggl.komposable.extensions.mergeWith
import com.toggl.komposable.extensions.pullback
import com.toggl.komposable.sample.todo.data.AppDatabase
import com.toggl.komposable.sample.todo.data.TodoDao
import com.toggl.komposable.sample.todo.data.UserSubscription
import com.toggl.komposable.sample.todo.edit.EditAction
import com.toggl.komposable.sample.todo.edit.EditReducer
import com.toggl.komposable.sample.todo.edit.EditState
import com.toggl.komposable.sample.todo.list.ListAction
import com.toggl.komposable.sample.todo.list.ListReducer
import com.toggl.komposable.sample.todo.list.ListState
import com.toggl.komposable.sample.todo.list.ListSubscription
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.scope.StoreScopeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TodoModule {

    @Provides
    fun appReducer(
        navigationReducer: NavigationReducer,
        authReducer: AuthReducer,
        listReducer: ListReducer,
        editReducer: EditReducer
    ): Reducer<AppState, AppAction> =
        combine(
            navigationReducer,
            authReducer,
            listReducer.pullback(
                mapToLocalState = { appState -> ListState(appState.todoList, appState.backStack) },
                mapToLocalAction = AppAction::unwrap,
                mapToGlobalState = { appState, listState -> appState.copy(todoList = listState.todoList, backStack = listState.backStack) },
                mapToGlobalAction = { listAction -> AppAction.List(listAction) }
            ),
            editReducer.pullback(
                mapToLocalState = { appState -> EditState(appState.editableTodo, appState.backStack) },
                mapToLocalAction = AppAction::unwrap,
                mapToGlobalState = { appState, editState -> appState.copy(editableTodo = editState.editableTodo, backStack = editState.backStack) },
                mapToGlobalAction = { listAction -> AppAction.Edit(listAction) }
            )
        )

    @Provides
    @Singleton
    fun appStore(
        reducer: Reducer<AppState, AppAction>,
        listSubscription: ListSubscription,
        userSubscription: UserSubscription,
        dispatcherProvider: DispatcherProvider,
        application: Application
    ): Store<AppState, AppAction> =
        createStore(
            initialState = AppState(),
            reducer = reducer,
            subscription = listSubscription mergeWith userSubscription,
            dispatcherProvider = dispatcherProvider,
            storeScopeProvider = application as StoreScopeProvider
        )

    @Provides
    @Singleton
    fun dispatcherProvider(): DispatcherProvider =
        DispatcherProvider(
            io = Dispatchers.IO,
            computation = Dispatchers.Default,
            main = Dispatchers.Main
        )

    @Provides
    @Singleton
    fun database(@ApplicationContext applicationContext: Context): AppDatabase =
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todo-app-database"
        ).build()

    @Provides
    @Singleton
    fun todoDao(database: AppDatabase): TodoDao =
        database.todoDao()
}

@Module
@InstallIn(ActivityRetainedComponent::class)
object AppViewModelModule {

    @Provides
    fun listStore(store: Store<AppState, AppAction>): Store<ListState, ListAction> =
        store.view(
            mapToLocalState = { appState -> ListState(appState.todoList, appState.backStack) },
            mapToGlobalAction = { listAction -> AppAction.List(listAction) }
        )

    @Provides
    fun editStore(store: Store<AppState, AppAction>): Store<EditState, EditAction> =
        store.view(
            mapToLocalState = { appState -> EditState(appState.editableTodo, appState.backStack) },
            mapToGlobalAction = { editAction -> AppAction.Edit(editAction) }
        )
}
