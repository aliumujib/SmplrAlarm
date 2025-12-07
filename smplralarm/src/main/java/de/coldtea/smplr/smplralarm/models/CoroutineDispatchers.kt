package de.coldtea.smplr.smplralarm.models

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Abstraction over coroutine dispatchers used by the library. This allows
 * host apps to plug in their own dispatchers for testing or customization,
 * and removes repeated references to Dispatchers.IO at call sites.
 */
interface SmplrAlarmDispatchers {
    val io: CoroutineDispatcher
}

object DefaultSmplrAlarmDispatchers : SmplrAlarmDispatchers {
    override val io: CoroutineDispatcher = Dispatchers.IO
}

object SmplrAlarmDispatchersHolder {
    @Volatile
    var dispatchers: SmplrAlarmDispatchers = DefaultSmplrAlarmDispatchers
}

/**
 * Public entry point for host apps/tests to override the dispatchers used
 * internally by SmplrAlarm. This is intentionally simple so it can be called
 * from application initializers or test setups.
 */
fun SmplrAlarmSetDispatchers(dispatchers: SmplrAlarmDispatchers) {
    SmplrAlarmDispatchersHolder.dispatchers = dispatchers
}

/**
 * Convenience helper for launching short-lived IO work on the library's
 * configured IO dispatcher.
 */
internal fun launchIo(block: suspend CoroutineScope.() -> Unit) =
    CoroutineScope(SmplrAlarmDispatchersHolder.dispatchers.io).launch(block = block)
