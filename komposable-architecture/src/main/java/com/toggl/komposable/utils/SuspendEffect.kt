package com.toggl.komposable.utils

import com.toggl.komposable.architecture.Effect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class SuspendEffect<out Action> : Effect<Action> {
    abstract suspend fun execute(): Action?

    final override fun run(): Flow<Action> = flow {
        execute()?.let { emit(it) }
    }
}
