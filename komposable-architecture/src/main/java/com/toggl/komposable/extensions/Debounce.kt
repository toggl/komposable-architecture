package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.withLock

fun <Action> Effect<Action>.debounce(
    id: Any,
    delayMillis: Long,
): Effect<Action> = Effect {
    this.run().debounce(delayMillis).onStart {
        mutex.withLock {
            cancellationJobs[id]?.forEach {
                it.cancel(EffectCancellationException.InFlightEffectsCancelled(id))
            }
            cancellationJobs.remove(id)

            val job = currentCoroutineContext()[Job]
            job?.let { cancellationJobs.getOrPut(id) { mutableSetOf() }.add(it) }
        }
        delay(delayMillis)
    }.cancellable()
}
