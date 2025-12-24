package de.coldtea.smplr.smplralarm.models

import de.coldtea.smplr.smplralarm.models.WeekDays

/**
 * Abstraction for computing the next alarm trigger time as epoch millis.
 *
 * All alarm time calculations in the library should go through this
 * interface so that behavior can be customized or swapped without
 * touching call sites.
 */
interface AlarmTimeCalculator {
    /**
     * Compute the next trigger time in epoch milliseconds.
     */
    fun computeNextTriggerTimeMillis(
        hour: Int,
        minute: Int,
        second: Int = 0,
        millis: Int = 0,
        weekDays: List<WeekDays>,
    ): Long
}
