package com.toggl.komposable.sample.todo

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.extensions.mutateWithoutEffects
import com.toggl.komposable.extensions.noEffect
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthReducer @Inject constructor() : Reducer<AppState, AppAction> {
    override fun reduce(state: Mutable<AppState>, action: AppAction): List<Effect<AppAction>> =
        when (action) {
            is AppAction.IdentityUpdated -> state.mutateWithoutEffects { copy(identity = action.newIdentity) }
            else -> noEffect()
        }
}
