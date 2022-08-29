package com.toggl.komposable.exceptions

/**
 * ExceptionHandler that never handles any exception but rethrows them instead.
 * @see ExceptionHandler
 */
class RethrowingExceptionHandler : ExceptionHandler {
    override suspend fun handleException(exception: Throwable): Boolean =
        throw exception
}
