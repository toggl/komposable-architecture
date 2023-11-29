package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val mutex = Mutex()
private val cancellationJobs: MutableMap<Any, MutableSet<Job>> = mutableMapOf()

/**
 * Makes an Effect cancellable by associating it with a given ID.
 * If 'cancelInFlight' is true, any previous effects with the same ID will be cancelled.
 *
 * @param id The identifier for this cancellable effect. Used to manage cancellation.
 * @param cancelInFlight If true, cancels any ongoing effects with the same ID.
 * @return A new Effect instance that is cancellable.
 */
fun <Action> Effect<Action>.cancellable(id: Any, cancelInFlight: Boolean = false): Effect<Action> =
    Effect {
        this().onStart {
            mutex.withLock {
                if (cancelInFlight) {
                    cancellationJobs[id]?.forEach { it.cancel() }
                    cancellationJobs.remove(id)
                }
                val job = currentCoroutineContext()[Job]
                job?.let { cancellationJobs.getOrPut(id) { mutableSetOf() }.add(it) }
            }
        }.cancellable()
    }

/**
 * Creates an Effect that cancels any ongoing effects associated with the given action ID.
 *
 * @param id The ID of the effects to be cancelled.
 * @return An Effect of Nothing that, when executed, will cancel the specified effects.
 */
fun Effect.Companion.cancel(id: Any): Effect<Nothing> =
    Effect {
        flow {
            mutex.withLock {
                cancellationJobs[id]?.forEach { it.cancel() }
                cancellationJobs.remove(id)
            }
        }
    }

/**
 * Creates an Effect that cancels all ongoing effects.
 * @return An Effect of Nothing that, when executed, will cancel all ongoing effects.
 */
fun Effect.Companion.cancelAll(): Effect<Nothing> =
    Effect {
        flow {
            mutex.withLock {
                cancellationJobs.values.flatten().forEach { it.cancel() }
                cancellationJobs.clear()
            }
        }
    }
