package com.toggl.komposable.architecture

/**
 * The result of a reducer's reduce method, containing the new state and a list of effects
 */
data class ReduceResult<out State, Action>(
    val state: State,
    val effect: Effect<Action>,
)
