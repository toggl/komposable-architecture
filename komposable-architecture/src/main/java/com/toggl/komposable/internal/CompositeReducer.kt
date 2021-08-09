package com.toggl.komposable.internal

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer

internal class CompositeReducer<State, Action>(private val reducers: List<Reducer<State, Action>>) :
    Reducer<State, Action> {
    override fun reduce(state: Mutable<State>, action: Action): List<Effect<Action>> =
        reducers.flatMap { it.reduce(state, action) }
}
