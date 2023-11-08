package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.experimental.ExperimentalTypeInference

fun <Action> Effect<Action>.merge(vararg effects: Effect<Action>): Effect<Action> =
    Effect {
        kotlinx.coroutines.flow.merge(actions(), *effects.map { it.actions() }.toTypedArray())
    }

/**
 * Transforms and effect by mapping the resulting action into a different type.
 * @param mapFn          The function to transform the returned action.
 * @return An effect whose return action type has been mapped.
 */
fun <T, R> Effect<T>.map(mapFn: (T) -> R): Effect<R> =
    Effect { actions().map { mapFn(it) } }

/**
 * Creates a list containing a single effect that when executed returns an action.
 * @param action          The action returned by the single effect.
 * @return A list of effects containing a single effect.
 */
fun <Action> effectOf(action: Action): Effect<Action> =
    Effect { flowOf(action) }

@Suppress("UNUSED_PARAMETER")
fun <Action, Key> Effect<Action>.cancellable(key: Key, cancelInFlight: Boolean = false): Effect<Action> =
    this // TODO implement this

@Suppress("UNUSED_PARAMETER")
fun <Action, Key> Effect.Companion.cancel(action: Key): Effect<Action> =
    Effect { emptyFlow() } // TODO implement this

fun <S, Action> S.withoutEffect(): ReduceResult<S, Action> =
    ReduceResult(this, NoEffect)

fun <S, Action> S.withEffect(effectBuilder: EffectBuilderScope.() -> Effect<Action>): ReduceResult<S, Action> =
    ReduceResult(this, EffectBuilderScope.effectBuilder())

object EffectBuilderScope {
    fun <Action> emitActions(vararg actions: Action): Effect<Action> =
        Effect { flowOf(*actions) }

    fun <Action> suspended(func: suspend () -> Action): Effect<Action> =
        Effect { flow { emit(func()) } }
    fun <Action> flow(actionFlow: Flow<Action>): Effect<Action> =
        Effect { actionFlow }

    @OptIn(ExperimentalTypeInference::class)
    fun <Action> callbackFlow(@BuilderInference block: suspend ProducerScope<Action>.() -> Unit): Effect<Action> =
        Effect { kotlinx.coroutines.flow.callbackFlow(block) }
}
