package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onStart

/**
 * Debounces an effect by delaying its execution by the specified amount of time.
 *  Note that this will only debounce effect if it is executed multiple times within the delay period.
 *  If effects with the same id are fired emitted faster than the delay indefinitely, they will never
 *  start emitting their actions.
 *  It does not affect the internal execution of the effect, but a debounced effect can be cancelled,
 *  cancelling it's internal execution.
 *
 * @param id The identifier for this debounced effect. Used to manage cancellation.
 * @param delayMillis The amount of time to delay the execution of the effect.
 * @return A new Effect instance that is debounced.
 */
fun <Action> Effect<Action>.debounce(
    id: Any,
    delayMillis: Long,
): Effect<Action> = Effect {
    this.run().onStart {
        delay(delayMillis)
    }
}.cancellable(id, true)
