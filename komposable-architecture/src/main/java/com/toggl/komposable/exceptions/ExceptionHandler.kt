package com.toggl.komposable.exceptions

interface ExceptionHandler {
    suspend fun handleException(exception: Throwable): Boolean
}