package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.experimental.ExperimentalTypeInference

/**
 * @param effects A vararg of Effect instances to be merged.
 * @return A new Effect instance that is a combination of the emissions from the provided Effects.
 */
fun <Action> Effect.Companion.merge(vararg effects: Effect<Action>): Effect<Action> =
    Effect {
        kotlinx.coroutines.flow.merge(*effects.map { it.run() }.toTypedArray())
    }

/**
 * @param effects A vararg of Effect instances to be merged with the current Effect.
 * @return A new Effect instance that combines the emissions of the current Effect and the provided Effects.
 */
fun <Action> Effect<Action>.merge(vararg effects: Effect<Action>): Effect<Action> =
    Effect {
        kotlinx.coroutines.flow.merge(run(), *effects.map { it.run() }.toTypedArray())
    }

/**
 * @param effect The Effect instance to be merged with the current Effect.
 * @return A new Effect instance combining the emissions from both the current and provided Effect.
 */
infix fun <Action> Effect<Action>.mergeWith(effect: Effect<Action>): Effect<Action> =
    Effect {
        kotlinx.coroutines.flow.merge(run(), effect.run())
    }

/**
 * Transforms and effect by mapping the resulting action into a different type.
 *
 * @param mapFn The function to transform the returned action.
 * @return An effect whose return action type has been mapped.
 */
inline fun <T, R> Effect<T>.map(crossinline transform: suspend (T) -> R): Effect<R> =
    if (this is NoEffect) {
        NoEffect
    } else {
        Effect { this.run().map { transform(it) } }
    }

/**
 * @return ReduceResult containing the current state and 'NoEffect'.
 */
fun <State, Action> State.withoutEffect(): ReduceResult<State, Action> =
    ReduceResult(this, NoEffect)

/**
 * @param id The identifier for the cancellation effect.
 * @return A ReduceResult containing the current state and a cancellation effect for the specified ID.
 * @see Effect.cancellable
 */
fun <State, Action> State.withCancelEffect(id: Any): ReduceResult<State, Action> =
    ReduceResult(this, Effect.cancel(id))

/**
 * @return A ReduceResult containing the current state and a cancellation effect for all ongoing effects.
 */
fun <State, Action> State.withCancelAllEffects(): ReduceResult<State, Action> =
    ReduceResult(this, Effect.cancelAll())

/**
 * @param effect An effect to be part of the ReduceResult.
 * @return ReduceResult containing the current state and the specified effect.
 */
fun <State, Action> State.withEffect(effect: Effect<Action>): ReduceResult<State, Action> =
    ReduceResult(this, effect)

/**
 * @param effect An effect to be part of the ReduceResult.
 * @param id ID to make the effect cancellable. If null, the effect is not cancellable.
 * @param cancelInFlight If true, any existing effect with the same ID will be cancelled.
 * @return ReduceResult containing the current state and the specified effect.
 */
fun <State, Action> State.withEffect(
    effect: Effect<Action>,
    id: Any,
    cancelInFlight: Boolean = false,
): ReduceResult<State, Action> =
    ReduceResult(this, effect.cancellable(id, cancelInFlight))

/**
 * @param effectBuilder The builder function for the effect to be returned.
 * @return ReduceResult containing the current state and the specified effect.
 */
fun <State, Action> State.withEffect(
    effectBuilder: Effect.Companion.() -> Effect<Action>,
): ReduceResult<State, Action> =
    ReduceResult(this, Effect.effectBuilder())

/**
 * @param id ID to make the effect cancellable. If null, the effect is not cancellable.
 * @param cancelInFlight If true, any existing effect with the same ID will be cancelled.
 * @param effectBuilder The builder function for the effect to be returned.
 * @return ReduceResult containing the current state and the specified effect.
 */
fun <State, Action> State.withEffect(
    id: Any,
    cancelInFlight: Boolean = false,
    effectBuilder: Effect.Companion.() -> Effect<Action>,
): ReduceResult<State, Action> =
    ReduceResult(this, Effect.effectBuilder().cancellable(id, cancelInFlight))

/**
 * @param actions Vararg of actions to be returned by the effect.
 * @return ReduceResult containing the current state and the specified effect.
 */
fun <State, Action> State.withEffect(vararg actions: Action): ReduceResult<State, Action> =
    ReduceResult(this, Effect.of(*actions))

/**
 * Returns a ReduceResult with the current state and a flow-based effect.
 *
 * @param flow The flow of actions to be executed as an effect.
 * @return ReduceResult containing the current state and the defined effect.
 */
fun <State, Action> State.withFlowEffect(flow: Flow<Action>): ReduceResult<State, Action> =
    ReduceResult(this, Effect.fromFlow(flow))

/**
 * Returns a ReduceResult with the current state and a flow-based effect.
 * The effect can be made cancellable using an ID.
 *
 * @param flow The flow of actions to be executed as an effect.
 * @param id ID to make the effect cancellable. If null, the effect is not cancellable.
 * @param cancelInFlight If true, any existing effect with the same ID will be cancelled.
 * @return ReduceResult containing the current state and the defined effect.
 */
fun <State, Action> State.withFlowEffect(
    flow: Flow<Action>,
    id: Any,
    cancelInFlight: Boolean = false,
): ReduceResult<State, Action> =
    ReduceResult(this, Effect.fromFlow(flow).cancellable(id, cancelInFlight))

/**
 * Returns a ReduceResult with the current state and a suspend function-based effect.
 *
 * @param func The suspend function that defines the action to be executed as an effect.
 * @return ReduceResult containing the current state and the defined effect.
 */
fun <State, Action : Any> State.withSuspendEffect(func: suspend () -> Action): ReduceResult<State, Action> =
    ReduceResult(this, Effect.fromSuspend(func))

/**
 * Returns a ReduceResult with the current state and a suspend function-based effect.
 * The effect can be cancellable and is defined by the provided suspend function.
 *
 * @param id ID for making the effect cancellable. If null, effect is not cancellable.
 * @param cancelInFlight If true, any existing effect with the same ID will be cancelled.
 * @param func The suspend function that defines the action to be executed as an effect.
 * @return ReduceResult containing the current state and the defined effect.
 */
fun <State, Action : Any> State.withSuspendEffect(
    id: Any,
    cancelInFlight: Boolean = false,
    func: suspend () -> Action,
): ReduceResult<State, Action> =
    ReduceResult(this, Effect.fromSuspend(func).cancellable(id, cancelInFlight))

/**
 * Returns a ReduceResult with the current state and a callback flow-based effect.
 *
 * @param block The callback block defining the flow of actions.
 * @return ReduceResult containing the current state and the defined effect.
 * @see kotlinx.coroutines.flow.callbackFlow
 */
@OptIn(ExperimentalTypeInference::class)
fun <State, Action> State.withProducerEffect(
    @BuilderInference block: suspend ProducerScope<Action>.() -> Unit,
): ReduceResult<State, Action> =
    ReduceResult(this, Effect.fromProducer(block))

/**
 * Returns a ReduceResult with the current state and a callback flow-based effect.
 * The effect can be made cancellable and is defined by a callback block.
 *
 * @param id ID to make the effect cancellable. If null, the effect is not cancellable.
 * @param cancelInFlight If true, any existing effect with the same ID will be cancelled.
 * @param block The callback block defining the flow of actions.
 * @return ReduceResult containing the current state and the defined effect.
 * @see kotlinx.coroutines.flow.callbackFlow
 */
@OptIn(ExperimentalTypeInference::class)
fun <State, Action> State.withProducerEffect(
    id: Any,
    cancelInFlight: Boolean = false,
    @BuilderInference block: suspend ProducerScope<Action>.() -> Unit,
): ReduceResult<State, Action> =
    ReduceResult(this, Effect.fromProducer(block).cancellable(id, cancelInFlight))
