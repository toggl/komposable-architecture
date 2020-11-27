package com.toggl.komposable.internal

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.map
import com.toggl.komposable.extensions.noEffect

internal class OptionalReducer<LocalState, GlobalState, LocalAction, GlobalAction>(
    private val innerReducer: Reducer<LocalState, LocalAction>,
    private val mapToLocalState: (GlobalState) -> LocalState?,
    private val mapToLocalAction: (GlobalAction) -> LocalAction?,
    private val mapToGlobalState: (GlobalState, LocalState?) -> GlobalState,
    private val mapToGlobalAction: (LocalAction) -> GlobalAction
) : Reducer<GlobalState, GlobalAction> {
    override fun reduce(
        state: Mutable<GlobalState>,
        action: GlobalAction
    ): List<Effect<GlobalAction>> {
        val localAction = mapToLocalAction(action) ?: return noEffect()

        var localState: LocalState = state
            .map(mapToLocalState, mapToGlobalState)
            .withValue<LocalState?> { this } ?: return noEffect()

        val localMutableValue = Mutable({ localState }) { localState = it }

        val effects = innerReducer
            .reduce(localMutableValue, localAction)
            .map { effect -> effect.map { action -> action?.run(mapToGlobalAction) } }

        state.mutate { mapToGlobalState(this, localState) }

        return effects
    }
}
