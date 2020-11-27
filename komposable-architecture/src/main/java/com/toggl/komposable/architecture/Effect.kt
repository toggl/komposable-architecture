package com.toggl.komposable.architecture
interface Effect<out Action> {
    suspend fun execute(): Action?
}
