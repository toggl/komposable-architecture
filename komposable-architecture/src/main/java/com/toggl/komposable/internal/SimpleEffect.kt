package com.toggl.komposable.internal

import com.toggl.komposable.architecture.Effect

internal class SimpleEffect<Action>(private val action: Action) : Effect<Action> {
    override suspend fun execute(): Action? = action
}
