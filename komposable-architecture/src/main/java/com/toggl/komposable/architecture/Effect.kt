package com.toggl.komposable.architecture

import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlin.experimental.ExperimentalTypeInference

/**
 * Effects are returned by reducers when they wish to produce a side effect.
 * This can be anything from cpu/io bound operations to changes that simply affect the UI.
 * @see Reducer.reduce
 */
fun interface Effect<out Action> {

    /**
     * Executes the effect. This operation can produce side effects, and it's the
     * responsibility of the class implementing this interface to change threads
     * to prevent blocking the UI when needed.
     * @return An action that will be sent again for further processing
     * @see Store.send
     */
    fun run(): Flow<Action>

    companion object {
        fun none(): Effect<Nothing> = NoEffect
        fun <Action> of(vararg actions: Action): Effect<Action> =
            Effect { flowOf(*actions) }

        fun <Action> fromFlow(flow: Flow<Action>): Effect<Action> =
            Effect { flow }

        fun <Action> fromSuspend(func: suspend () -> Action): Effect<Action> =
            Effect { func.asFlow() }

        @OptIn(ExperimentalTypeInference::class)
        fun <Action> fromProducer(@BuilderInference block: suspend ProducerScope<Action>.() -> Unit): Effect<Action> =
            Effect { kotlinx.coroutines.flow.callbackFlow(block) }
    }
}

object NoEffect : Effect<Nothing> {
    override fun run(): Flow<Nothing> = emptyFlow()
}
