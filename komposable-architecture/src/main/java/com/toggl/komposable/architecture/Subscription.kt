package com.toggl.komposable.architecture

import kotlinx.coroutines.flow.Flow

/**
 * Subscriptions are like long-running effects that can emit actions without ever stopping.
 * A common use for subscriptions is to leverage database observing to ensure the entities
 * in your app that are backed by a database are always up-to-date.
 * @see Effect
 */
fun interface Subscription<State, Action : Any> {

    /**
     * Returns a Flow of actions that get handled by the store as they get emitted
     * @param state The flow of state as provided by the store
     * @see Store
     */
    fun subscribe(state: Flow<State>): Flow<Action>
}
