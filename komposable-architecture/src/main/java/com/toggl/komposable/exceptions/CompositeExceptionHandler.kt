package com.toggl.komposable.exceptions

class CompositeExceptionHandler(private val innerHandlers: List<ExceptionHandler>) : ExceptionHandler {
    override suspend fun handleException(exception: Throwable): Boolean {
        for (handler in innerHandlers) {
            val handled = handler.handleException(exception)
            if (handled) return true
        }

        return false
    }
}