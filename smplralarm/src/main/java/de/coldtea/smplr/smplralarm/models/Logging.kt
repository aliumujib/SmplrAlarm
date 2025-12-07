package de.coldtea.smplr.smplralarm.models

import timber.log.Timber

/**
 * Pluggable logger abstraction for SmplrAlarm. Library code should depend on
 * this interface instead of Timber directly so host apps can provide custom
 * logging/analytics implementations.
 */
interface SmplrAlarmLogger {
    fun v(message: String, throwable: Throwable? = null)
    fun d(message: String, throwable: Throwable? = null)
    fun i(message: String, throwable: Throwable? = null)
    fun w(message: String, throwable: Throwable? = null)
    fun e(message: String, throwable: Throwable? = null)
}

/**
 * Default logger implementation backed by Timber. Used when callers do not
 * provide a custom logger.
 */
object DefaultSmplrAlarmLogger : SmplrAlarmLogger {
    override fun v(message: String, throwable: Throwable?) {
        Timber.v(throwable, message)
    }

    override fun d(message: String, throwable: Throwable?) {
        Timber.d(throwable, message)
    }

    override fun i(message: String, throwable: Throwable?) {
        Timber.i(throwable, message)
    }

    override fun w(message: String, throwable: Throwable?) {
        Timber.w(throwable, message)
    }

    override fun e(message: String, throwable: Throwable?) {
        Timber.e(throwable, message)
    }
}

/**
 * Global logger holder used by components that cannot easily receive a logger
 * via constructor parameters (e.g., BroadcastReceivers, extension functions).
 *
 * SmplrAlarmAPI will set this to the logger it is constructed with, so host
 * apps can override logging behavior.
 */
object SmplrAlarmLoggerHolder {
    @Volatile
    var logger: SmplrAlarmLogger = DefaultSmplrAlarmLogger
}
