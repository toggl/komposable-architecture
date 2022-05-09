package com.toggl.komposable.architecture

/**
 * Effects are returned by reducers when they wish to produce a side effect
 * This can be anything from cpu/io bound operations to changes that simply affect the UI
 * @see Reducer.reduce
 */
interface Effect<out Action> {

    /**
     * Executes the effect. This operation can produce side effects and it's the
     * responsibility of the class implementing this interface to change threads
     * to prevent blocking the UI when needed
     * @return An action that will be sent again for further processing
     * @see Store.send
     */
    suspend fun execute(): Action?
}
