package com.toggl.komposable.internal

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import com.toggl.komposable.architecture.Reducer
import com.toggl.komposable.architecture.Store
import com.toggl.komposable.architecture.Subscription
import com.toggl.komposable.exceptions.ExceptionHandler
import com.toggl.komposable.extensions.mergeWith
import com.toggl.komposable.scope.DispatcherProvider
import com.toggl.komposable.scope.StoreScopeProvider
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
    private val sendFn: (List<Action>) -> Unit,
) : Store<State, Action> {

    override fun <ViewState, ViewAction : Any> view(
        mapToLocalState: (State) -> ViewState,
        mapToGlobalAction: (ViewAction) -> Action?,
    ): Store<ViewState, ViewAction> = MutableStateFlowStore(
        state = state.map { mapToLocalState(it) }.distinctUntilChanged(),
        sendFn = { actions ->
            val globalActions = actions.mapNotNull(mapToGlobalAction)
            sendFn(globalActions)
        },
    )

    override fun <ViewState : Any, ViewAction : Any> optionalView(
        mapToLocalState: (State) -> ViewState?,
        mapToGlobalAction: (ViewAction) -> Action?,
    ): Store<ViewState, ViewAction> = MutableStateFlowStore(
        state = state.mapNotNull { mapToLocalState(it) }.distinctUntilChanged(),
        sendFn = { actions ->
            val globalActions = actions.mapNotNull(mapToGlobalAction)
            sendFn(globalActions)
        },
    )

    companion object {
        fun <State, Action : Any> create(
            initialState: State,
            reducer: Reducer<State, Action>,
            subscription: Subscription<State, Action>,
            exceptionHandler: ExceptionHandler,
            storeScopeProvider: StoreScopeProvider,
            dispatcherProvider: DispatcherProvider,
        ): Store<State, Action> {
            val storeScope = storeScopeProvider.getStoreScope()
            val state = MutableStateFlow(initialState)
            val noEffect = NoEffect

            lateinit var send: (List<Action>) -> Unit
            send = { actions ->
                storeScope.launch(context = dispatcherProvider.main) {
                    val result: ReduceResult<State, Action> = actions.fold(ReduceResult(state.value, noEffect)) { accResult, action ->
                        try {
                            val (nextState, nextEffect) = reducer.reduce(accResult.state, action)
                            return@fold ReduceResult(nextState, accResult.effect mergeWith nextEffect)
                        } catch (e: Throwable) {
                            ReduceResult(accResult.state, exceptionHandler.handleReduceException(e))
                        }
                    }

                    state.value = result.state

                    try {
                        result.effect()
                            .onEach { action -> send(listOf(action)) }
                            .launchIn(storeScope)
                    } catch (e: Throwable) {
                        exceptionHandler.handleEffectException(e)
                    }
                }
            }

            try {
                subscription
                    .subscribe(state)
                    .onEach { action -> send(listOf(action)) }
                    .launchIn(storeScope)
            } catch (e: Throwable) {
                runBlocking {
                    exceptionHandler.handleSubscriptionException(e)
                }
            }
            return MutableStateFlowStore(state, send)
        }

        private suspend fun ExceptionHandler.handleReduceException(exception: Throwable): Effect<Nothing> =
            if (handleException(exception)) {
                NoEffect
            } else {
                throw exception
            }

        private suspend fun ExceptionHandler.handleEffectException(exception: Throwable): Nothing? =
            if (handleException(exception)) {
                null
            } else {
                throw exception
            }

        private suspend fun ExceptionHandler.handleSubscriptionException(exception: Throwable) {
            if (handleException(exception)) return
            throw exception
        }
    }

    override fun send(actions: List<Action>) {
        if (actions.isEmpty()) return

        sendFn(actions)
    }

    @Deprecated("Use send(List<Action>)", replaceWith = ReplaceWith("this.send(actions)"))
    override fun dispatch(actions: List<Action>) = send(actions)
}
