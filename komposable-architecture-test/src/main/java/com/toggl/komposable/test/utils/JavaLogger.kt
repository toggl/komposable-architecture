package com.toggl.komposable.test.utils

class JavaLogger(private val logger: java.util.logging.Logger) : Logger {
    override fun log(level: LogLevel, message: String?, error: Throwable?) {
        when (level) {
            LogLevel.Info -> logger.info(message)
            LogLevel.Warning -> logger.warning(message)
            LogLevel.Error -> logger.severe(message?.let { "$it: ${error?.message}" } ?: error?.message)
        }
    }
}
