package com.toggl.komposable.internal

import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.map
import com.toggl.komposable.extensions.noEffect

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
            ?: return ReduceResult(state, noEffect())

        val newLocalState = innerReducer.reduce(mapToLocalState(state), localAction)

        return ReduceResult(
            mapToGlobalState(state, newLocalState.state),
            newLocalState.effects.map { effects -> effects.map { e -> e?.run(mapToGlobalAction) } },
        )
    }
}
