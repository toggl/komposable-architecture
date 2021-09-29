package com.toggl.komposable.sample.todo

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.extensions.mutateWithoutEffects
import com.toggl.komposable.sample.todo.data.EditableTodoItem
import com.toggl.komposable.sample.todo.data.Identity
import com.toggl.komposable.sample.todo.data.TodoItem

data class AppState(
    val todoList: List<TodoItem> = emptyList(),
    val editableTodo: EditableTodoItem = EditableTodoItem(title = "", description = ""),
    val identity: Identity = Identity.Unknown,
    override val backStack: BackStack = listOf(AppDestination.List)
) : BackStackAwareState<AppState> {
    override fun changeBackStack(backStack: BackStack): AppState =
        copy(backStack = backStack)
}

typealias BackStack = List<AppDestination>

fun BackStack.push(destination: AppDestination) =
    this + destination

fun BackStack.pop() =
    this.dropLast(1)

interface BackStackAwareState<T> {
    val backStack: BackStack
    fun popBackStack(): T = changeBackStack(backStack.pop())
    fun changeBackStack(backStack: BackStack): T
}

fun <State : BackStackAwareState<State>, Action> Mutable<State>.popBackStackWithoutEffects(): List<Effect<Action>> =
    mutateWithoutEffects { popBackStack() }
