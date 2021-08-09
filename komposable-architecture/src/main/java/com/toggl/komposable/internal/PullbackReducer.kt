package com.toggl.komposable.internal

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.map
import com.toggl.komposable.extensions.noEffect

internal class PullbackReducer<LocalState, GlobalState, LocalAction, GlobalAction>(
    private val innerReducer: Reducer<LocalState, LocalAction>,
    private val mapToLocalState: (GlobalState) -> LocalState,
    private val mapToLocalAction: (GlobalAction) -> LocalAction?,
    private val mapToGlobalState: (GlobalState, LocalState) -> GlobalState,
    private val mapToGlobalAction: (LocalAction) -> GlobalAction
) : Reducer<GlobalState, GlobalAction> {
    override fun reduce(
        state: Mutable<GlobalState>,
        action: GlobalAction
    ): List<Effect<GlobalAction>> {
        val localAction = mapToLocalAction(action)
            ?: return noEffect()

        return innerReducer
            .reduce(state.map(mapToLocalState, mapToGlobalState), localAction)
            .map { effect -> effect.map { action -> action?.run(mapToGlobalAction) } }
    }
}
