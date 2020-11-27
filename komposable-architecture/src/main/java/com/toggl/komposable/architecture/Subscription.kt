package com.toggl.komposable.architecture

import kotlinx.coroutines.flow.Flow

fun interface Subscription<State, Action : Any> {

    fun subscribe(state: Flow<State>): Flow<Action>
}