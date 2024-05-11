package com.toggl.komposable.extensions

import com.toggl.komposable.architecture.Effect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart

fun <Action> Effect<Action>.debounce(
    id: Any,
    delayMillis: Long,
): Effect<Action> = Effect {
    this.run().debounce(delayMillis).onStart {
        delay(delayMillis)
    }
}.cancellable(id, true)
