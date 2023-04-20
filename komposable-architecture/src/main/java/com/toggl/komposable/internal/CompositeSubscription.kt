package com.toggl.komposable.internal

import com.toggl.komposable.architecture.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge

internal class CompositeSubscription<State, Action : Any>(
    private val subscriptions: Collection<Subscription<State, Action>>,
) : Subscription<State, Action> {
    override fun subscribe(state: Flow<State>): Flow<Action> {
        val subscriptionFlows: List<Flow<Action>> = subscriptions.map { sub ->
            sub.subscribe(state.distinctUntilChanged())
        }
        return subscriptionFlows.merge()
    }
}
