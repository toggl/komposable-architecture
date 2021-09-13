package com.toggl.komposable.sample.todo

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.extensions.mutateWithoutEffects
import com.toggl.komposable.sample.todo.data.EditableTodoItem
import com.toggl.komposable.sample.todo.data.TodoItem

data class AppState(
    val todoList: List<TodoItem> = emptyList(),
    val editableTodo: EditableTodoItem = EditableTodoItem(title = "", description = ""),
    override val backStack: BackStack = listOf(AppDestination.List)
) : BackStackAwareState<AppState> {
    override fun changeBackStack(route: BackStack): AppState =
        copy(backStack = route)
}

typealias BackStack = List<AppDestination>

fun BackStack.push(destination: AppDestination) =
    this + destination

fun BackStack.pop() =
    this.dropLast(1)

interface BackStackAwareState<T> {
    val backStack: BackStack
    fun popBackStack(): T = changeBackStack(backStack.pop())
    fun changeBackStack(route: BackStack): T
}

fun <State : BackStackAwareState<State>, Action> Mutable<State>.popBackStackWithoutEffects(): List<Effect<Action>> =
    mutateWithoutEffects { popBackStack() }
