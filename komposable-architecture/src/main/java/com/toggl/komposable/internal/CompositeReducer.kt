package com.toggl.komposable.internal

import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.mergeWith

internal class CompositeReducer<State, Action>(private val reducers: List<Reducer<State, Action>>) :
    Reducer<State, Action> {
    override fun reduce(state: State, action: Action): ReduceResult<State, Action> =
        reducers.fold(ReduceResult(state, NoEffect)) { accResult, reducer ->
            val result = reducer.reduce(accResult.state, action)
            ReduceResult(result.state, accResult.effect mergeWith result.effect)
        }
}
