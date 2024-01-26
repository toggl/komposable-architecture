package com.toggl.komposable.test.utils

class NoopLogger : Logger {
    override fun log(level: LogLevel, message: String?, error: Throwable?) {
        // no-op
    }
}
