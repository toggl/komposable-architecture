package com.toggl.komposable.exceptions

/**
 * ExceptionHandler that forwards the exceptions to all innerHandlers.
 * The exception is no longer forwarded as soon as any handler returns true.
 * @see ExceptionHandler
 */
class CompositeExceptionHandler(private val innerHandlers: List<ExceptionHandler>) : ExceptionHandler {
    override suspend fun handleException(exception: Throwable): Boolean {
        for (handler in innerHandlers) {
            val handled = handler.handleException(exception)
            if (handled) return true
        }

        return false
    }
}
