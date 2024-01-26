package com.toggl.komposable.test.utils

enum class LogLevel {
    Info,
    Warning,
    Error,
}

interface Logger {
    fun log(level: LogLevel, message: String?, error: Throwable? = null)
}

fun Logger.info(message: String) = log(LogLevel.Info, message)
fun Logger.warning(message: String) = log(LogLevel.Warning, message)
fun Logger.error(message: String?, error: Throwable?) = log(LogLevel.Error, message, error)
