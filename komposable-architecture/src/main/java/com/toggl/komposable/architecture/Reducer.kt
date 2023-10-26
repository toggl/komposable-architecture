package com.toggl.komposable.architecture

/**
 * Types responsible for handling actions and mutating the state.
 */
fun interface Reducer<State, Action> {
    /**
     * The reduce function is responsible for transforming actions into state changes.
     * Referential transparency is of utmost importance when it comes to the reduce
     * method, so absolutely no side effects or long-running operations should happen here.
     * The reduce method should instead rely on returning effects which can then signal
     * their completion via actions which will be scheduled for processing.
     * @return A ReduceResult containing the new state and a list of effects that should
     * be executed immediately after reduce method finishes its job
     */
    fun reduce(state: State, action: Action): ReduceResult<State, Action>
}
