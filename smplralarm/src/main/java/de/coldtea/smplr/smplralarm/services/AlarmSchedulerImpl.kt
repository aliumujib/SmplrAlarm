package de.coldtea.smplr.smplralarm.services

import de.coldtea.smplr.smplralarm.extensions.getTimeExactForAlarmInMilliseconds
import de.coldtea.smplr.smplralarm.models.AlarmDefinition
import de.coldtea.smplr.smplralarm.models.AlarmScheduler
import de.coldtea.smplr.smplralarm.models.AlarmStore
import de.coldtea.smplr.smplralarm.models.WeekDays
import de.coldtea.smplr.smplralarm.models.launchIo
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Default implementation of [AlarmScheduler] that delegates to [AlarmService].
 *
 * This is intentionally minimal for the first refactor phase – we only
 * introduce the abstraction without changing existing behavior.
 */
class AlarmSchedulerImpl(
    private val alarmService: AlarmService,
    private val store: AlarmStore,
) : AlarmScheduler {

    override fun schedule(
        id: Int,
        hour: Int,
        minute: Int,
        second: Int,
        weekDays: List<WeekDays>,
    ) {
        alarmService.setAlarm(
            requestCode = id,
            hour = hour,
            min = minute,
            weekDays = weekDays,
            second = second,
        )

        updateNextTriggerTime(id, hour, minute, weekDays)
    }

    override fun rescheduleTomorrow(definition: AlarmDefinition) {
        // Currently we reschedule using the same time-of-day and weekdays,
        // relying on the shared Calendar helper to compute the next
        // occurrence and update nextTriggerTime in the store.
        schedule(
            id = definition.id,
            hour = definition.hour,
            minute = definition.minute,
            second = definition.second,
            weekDays = definition.weekdays,
        )
    }

    override fun renew(definition: AlarmDefinition) {
        // Same behavior as rescheduleTomorrow: re-schedule with current
        // data, letting the time-calculation helper and nextTriggerTime
        // update determine the actual next fire time.
        schedule(
            id = definition.id,
            hour = definition.hour,
            minute = definition.minute,
            second = definition.second,
            weekDays = definition.weekdays,
        )
    }

    private fun updateNextTriggerTime(
        id: Int,
        hour: Int,
        minute: Int,
        weekDays: List<WeekDays>,
    ) {
        val nextTime = Calendar.getInstance()
            .getTimeExactForAlarmInMilliseconds(hour, minute, weekDays)

        launchIo {
            val current = store.get(id) ?: return@launchIo
            store.update(current.copy(nextTriggerTime = nextTime))
        }
    }

    override fun cancel(id: Int) {
        alarmService.cancelAlarm(id)
    }

    override fun exists(id: Int): Boolean {
        return alarmService.alarmExist(id)
    }
}
