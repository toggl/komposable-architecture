package com.toggl.komposable.exceptions

class RethrowingExceptionHandler : ExceptionHandler {
    override suspend fun handleException(exception: Throwable): Boolean =
        throw exception
}