package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import com.toggl.komposable.architecture.NoEffect
import com.toggl.komposable.architecture.ReduceResult
🚧import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.experimental.ExperimentalTypeInference

/**
 * Merges multiple Effect instances into a single Effect.
 * This function is useful for combining the emissions of various Effects into one stream.
 * When the resulting Effect is executed, it will emit actions from all the merged Effects in the order they occur.
 *
 * @param effects A vararg of Effect instances to be merged with the current Effect.
 * @return A new Effect instance that combines the emissions of the current Effect and the provided Effects.
 */
fun <Action> Effect<Action>.merge(vararg effects: Effect<Action>): Effect<Action> =
    Effect {
        kotlinx.coroutines.flow.merge(invoke(), *effects.map { it.invoke() }.toTypedArray())
    }

/**
 * Transforms and effect by mapping the resulting action into a different type.
 *
 * @param mapFn The function to transform the returned action.
 * @return An effect whose return action type has been mapped.
 */
fun <T, R> Effect<T>.map(mapFn: (T) -> R): Effect<R> =
    if (this is NoEffect) {
        NoEffect
    } else {
        Effect { this().map { mapFn(it) } }
    }

/**
 * Creates an Effect which emits a sequence of actions.
 *
 * @param actions A vararg of actions to be emitted.
 * @return An Effect instance that, when executed, will emit the provided actions in the order they were given.
 */
fun <Action> effectOf(vararg actions: Action): Effect<Action> =
    Effect { flowOf(*actions) }

/**
 * @return ReduceResult containing the current state and 'NoEffect'.
 */
fun <State, Action> State.withoutEffect(): ReduceResult<State, Action> =
    ReduceResult(this, NoEffect)

/**
 * @param actions Vararg of actions to be returned by the effect.
 * @return ReduceResult containing the current state and the specified effect.
 */
fun <State, Action> State.withEffect(vararg actions: Action): ReduceResult<State, Action> =
    ReduceResult(this, effectOf(*actions))

/**
 * Returns a ReduceResult with the current state and a flow-based effect.
 * The effect can be made cancellable using an ID.
 *
 * @param flow The flow of actions to be executed as an effect.
 * @param id Optional ID to make the effect cancellable. If null, the effect is not cancellable.
 * @param cancelInFlight If true, any existing effect with the same ID will be cancelled.
 * @return ReduceResult containing the current state and the defined effect.
 */
fun <State, Action> State.withEffect(
    flow: Flow<Action>,
    id: Any? = null,
    cancelInFlight: Boolean = false,
): ReduceResult<State, Action> =
    ReduceResult(this, Effect { flow }.maybeCancellable(id, cancelInFlight))

/**
 * Returns a ReduceResult with the current state and a suspend function-based effect.
 * The effect can be cancellable and is defined by the provided suspend function.
 *
 * @param id Optional ID for making the effect cancellable. If null, effect is not cancellable.
 * @param cancelInFlight If true, any existing effect with the same ID will be cancelled.
 * @param func The suspend function that defines the action to be executed as an effect.
 * @return ReduceResult containing the current state and the defined effect.
 */
fun <State, Action : Any> State.withSuspendEffect(
    id: Any? = null,
    cancelInFlight: Boolean = false,
    func: suspend () -> Action,
): ReduceResult<State, Action> =
    ReduceResult(this, Effect { func.asFlow() }.maybeCancellable(id, cancelInFlight))

/**
 * Returns a ReduceResult with the current state and a callback flow-based effect.
 * The effect can be made cancellable and is defined by a callback block.
 *
 * @param id Optional ID to make the effect cancellable. If null, the effect is not cancellable.
 * @param cancelInFlight If true, any existing effect with the same ID will be cancelled.
 * @param block The callback block defining the flow of actions.
 * @return ReduceResult containing the current state and the defined effect.
 * @see kotlinx.coroutines.flow.callbackFlow
 */
@OptIn(ExperimentalTypeInference::class)
fun <State, Action> State.withCallbackEffect(
    id: Any? = null,
    cancelInFlight: Boolean = false,
    @BuilderInference block: suspend ProducerScope<Action>.() -> Unit,
): ReduceResult<State, Action> =
    ReduceResult(this, Effect { kotlinx.coroutines.flow.callbackFlow(block) }.maybeCancellable(id, cancelInFlight))

private fun <Action> Effect<Action>.maybeCancellable(id: Any? = null, cancelInFlight: Boolean = false): Effect<Action> =
    if (id == null) {
        this
    } else {
        this.cancellable(id, cancelInFlight)
    }
