package com.toggl.komposable.internal

import com.toggl.komposable.architecture.Effect

internal class MapEffect<T, R>(
    private val innerEffect: Effect<T>,
    private val mapFn: (T?) -> R?
) : Effect<R> {
    override suspend fun execute(): R? =
        innerEffect.execute()?.run(mapFn)
}
