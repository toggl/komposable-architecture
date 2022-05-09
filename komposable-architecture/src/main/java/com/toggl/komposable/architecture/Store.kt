package com.toggl.komposable.architecture

import kotlinx.coroutines.flow.Flow

/**
 * A Store holds a reducer and does all orchestration for ensuring that actions are properly
 * forwarded, state is correctly mutated and effects are properly executed
 * You can create one using the createStore function
 * @see com.toggl.komposable.extensions.createStore
 */
interface Store<State, Action : Any> {

    /**
     * A flow that emits whenever the state of the application changes
     */
    val state: Flow<State>

    /**
     * Sends actions to be processed by the internal reducer
     * If multiple actions are sent, the state will still only emit
     * once all actions are processed in a batch
     * @see state
     */
    fun send(actions: List<Action>)

    /**
     * Convenience method to send a single action to be processed by the internal reducer
     * @see Store.send
     */
    fun send(action: Action) =
        send(listOf(action))

    /**
     * Sends actions to be processed by the internal reducer
     * If multiple actions are dispatched, the state will still only emit
     * once all actions are processed in a batch
     * @see state
     */
    @Deprecated("Use send(List<Action>)", ReplaceWith("this.send(actions)"))
    fun dispatch(actions: List<Action>)

    /**
     * Convenience method to send a single action to be processed by the internal reducer
     * @see Store.dispatch
     */
    @Deprecated("Use send(Action)", ReplaceWith("this.send(action)"))
    fun dispatch(action: Action) =
        send(listOf(action))

    /**
     * Transforms this store in a more specific store that can only emit
     * a subset of its actions and access a subset of its state
     * @param mapToLocalState Function to transform the global state into the local state
     * @param mapToGlobalAction Function to transform the local action into a global action for
     * forwarding to the parent store
     */
    fun <ViewState, ViewAction : Any> view(
        mapToLocalState: (State) -> ViewState,
        mapToGlobalAction: (ViewAction) -> Action?
    ): Store<ViewState, ViewAction>

    /**
     * Transforms this store in a more specific store that can only emit
     * a subset of its actions and access a subset of its state.
     * The difference between this and the [view] method is that optionalView allows for
     * the state to be nullable, which prevents state propagation
     * @param mapToLocalState Function to transform the global state into the local state
     * @param mapToGlobalAction Function to transform the local action into a global action for
     * forwarding to the parent store
     */
    fun <ViewState : Any, ViewAction : Any> optionalView(
        mapToLocalState: (State) -> ViewState?,
        mapToGlobalAction: (ViewAction) -> Action?
    ): Store<ViewState, ViewAction>
}
