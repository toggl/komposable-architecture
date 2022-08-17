package com.toggl.komposable.exceptions

/**
 * ExceptionHandler that always marks exceptions as handled.
 * @see ExceptionHandler
 */
class RelaxedExceptionHandler : ExceptionHandler {
    override suspend fun handleException(exception: Throwable): Boolean = true
}
