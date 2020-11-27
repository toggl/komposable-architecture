package com.toggl.komposable.exceptions

class RelaxedExceptionHandler : ExceptionHandler {
    override suspend fun handleException(exception: Throwable): Boolean = true
}