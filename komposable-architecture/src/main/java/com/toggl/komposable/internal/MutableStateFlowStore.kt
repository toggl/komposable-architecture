package com.toggl.komposable.internal

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.Mutable
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.architecture.Subscription
import com.toggl.komposable.exceptions.ExceptionHandler
import com.toggl.komposable.extensions.noEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

internal class MutableStateFlowStore<State, Action : Any> private constructor(
    override val state: Flow<State>,
    private val dispatchFn: (List<Action>) -> Unit
) : Store<State, Action> {

    override fun <ViewState, ViewAction : Any> view(
        mapToLocalState: (State) -> ViewState,
        mapToGlobalAction: (ViewAction) -> Action?
    ): Store<ViewState, ViewAction> = MutableStateFlowStore(
        state = state.map { mapToLocalState(it) }.distinctUntilChanged(),
        dispatchFn = { actions ->
            val globalActions = actions.mapNotNull(mapToGlobalAction)
            dispatchFn(globalActions)
        }
    )

    override fun <ViewState : Any, ViewAction : Any> optionalView(
        mapToLocalState: (State) -> ViewState?,
        mapToGlobalAction: (ViewAction) -> Action?
    ): Store<ViewState, ViewAction> = MutableStateFlowStore(
        state = state.mapNotNull { mapToLocalState(it) }.distinctUntilChanged(),
        dispatchFn = { actions ->
            val globalActions = actions.mapNotNull(mapToGlobalAction)
            dispatchFn(globalActions)
        }
    )

    companion object {
        fun <State, Action : Any> create(
            initialState: State,
            reducer: Reducer<State, Action>,
            subscription: Subscription<State, Action>,
            storeScope: CoroutineScope,
            exceptionHandler: ExceptionHandler
        ): Store<State, Action> {
            val state = MutableStateFlow(initialState)

            lateinit var dispatch: (List<Action>) -> Unit
            dispatch = { actions ->
                storeScope.launch {
                    var tempState = state.value
                    val mutableValue = Mutable({ tempState }) { tempState = it }

                    val effects = actions.flatMap { action ->
                        try {
                            reducer.reduce(mutableValue, action)
                        } catch (e: Throwable) {
                            exceptionHandler.handleReduceException(e)
                        }
                    }
                    state.value = tempState

                    val effectActions = effects.mapNotNull { effect ->
                        try {
                            effect.execute()
                        } catch (e: Throwable) {
                            exceptionHandler.handleEffectException(e)
                        }
                    }
                    if (effectActions.isEmpty()) return@launch
                    dispatch(effectActions)
                }
            }

            try {
                subscription
                    .subscribe(state)
                    .onEach { action -> dispatch(listOf(action)) }
                    .launchIn(storeScope)
            } catch (e: Throwable) {
                runBlocking {
                    exceptionHandler.handleSubscriptionException(e)
                }
            }
            return MutableStateFlowStore(state, dispatch)
        }

        private suspend fun ExceptionHandler.handleReduceException(exception: Throwable): List<Effect<Nothing>> =
            if (handleException(exception)) noEffect()
            else throw exception

        private suspend fun ExceptionHandler.handleEffectException(exception: Throwable): Nothing? =
            if (handleException(exception)) null
            else throw exception

        private suspend fun ExceptionHandler.handleSubscriptionException(exception: Throwable) {
            if (handleException(exception)) return
            throw exception
        }
    }

    override fun dispatch(actions: List<Action>) {
        if (actions.isEmpty()) return

        dispatchFn(actions)
    }
}
