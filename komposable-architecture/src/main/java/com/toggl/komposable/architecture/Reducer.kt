package com.toggl.komposable.architecture
fun interface Reducer<State, Action> {
    fun reduce(state: Mutable<State>, action: Action): List<Effect<Action>>
}