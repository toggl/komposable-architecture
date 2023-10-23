package com.toggl.komposable.internal

import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.noEffect

internal class CompositeReducer<State, Action>(private val reducers: List<Reducer<State, Action>>) :
    Reducer<State, Action> {
    override fun reduce(state: State, action: Action): ReduceResult<State, Action> =
        reducers.fold(ReduceResult(state, noEffect())) { accResult, reducer ->
            val result = reducer.reduce(accResult.state, action)
            ReduceResult(result.state, accResult.effects + result.effects)
        }
}
