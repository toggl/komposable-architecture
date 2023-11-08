package com.toggl.komposable.internal

import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.map

internal class PullbackReducer<LocalState, GlobalState, LocalAction, GlobalAction>(
    private val innerReducer: Reducer<LocalState, LocalAction>,
    private val mapToLocalState: (GlobalState) -> LocalState,
    private val mapToLocalAction: (GlobalAction) -> LocalAction?,
    private val mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    private val mapToGlobalAction: (LocalAction) -> GlobalAction,
) : Reducer<GlobalState, GlobalAction> {
    override fun reduce(
        state: GlobalState,
        action: GlobalAction,
    ): ReduceResult<GlobalState, GlobalAction> {
        val localAction = mapToLocalAction(action)
            ?: return ReduceResult(state, NoEffect)

        val localResult = innerReducer.reduce(mapToLocalState(state), localAction)

        return ReduceResult(
            mapToGlobalState(state, localResult.state),
            localResult.effect.map(mapToGlobalAction),
        )
    }
}
