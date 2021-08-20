package com.toggl.komposable.sample.todo

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.extensions.mutateWithoutEffects

data class AppState(
    val todoList: List<TodoItem> = emptyList(),
    val editableTodo: EditableTodoItem = EditableTodoItem("", ""),
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

fun <State : BackStackAwareState<State>> Mutable<State>.popBackStack(): Mutable<State> {
    mutate { popBackStack() }
    return this
}

fun <State : BackStackAwareState<State>, Action> Mutable<State>.navigateWithoutEffects(route: List<AppDestination>): List<Effect<Action>> =
    mutateWithoutEffects {
        changeBackStack(route)
    }
