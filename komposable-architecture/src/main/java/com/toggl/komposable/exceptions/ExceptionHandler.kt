package com.toggl.komposable.exceptions

/**
 * Allows one to inject custom exception handling in a store.
 * @see com.toggl.komposable.architecture.Store
 */
interface ExceptionHandler {

    /**
     * Called whenever an exception is thrown during the reduce process.
     * @return true if the exception was handled, false if it should be propagated.
     * @see com.toggl.komposable.architecture.Store
     */
    suspend fun handleException(exception: Throwable): Boolean
}
