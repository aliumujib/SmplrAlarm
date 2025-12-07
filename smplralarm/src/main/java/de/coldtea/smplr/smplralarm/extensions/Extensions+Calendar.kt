package de.coldtea.smplr.smplralarm.extensions

import android.os.Build.VERSION
import de.coldtea.smplr.smplralarm.models.WeekDays
import de.coldtea.smplr.smplralarm.models.SmplrAlarmLoggerHolder
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.*

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal fun Calendar.getTimeExactForAlarmInMilliseconds(
    hour: Int,
    minute: Int,
    weekDays: List<WeekDays>
): Long {
    return getTimeExactForAlarm(hour, minute, 0, 0, weekDays).timeInMillis
}

internal fun Calendar.getTimeExactForAlarmInMilliseconds(
    hour: Int,
    minute: Int,
    second: Int,
    weekDays: List<WeekDays>
): Long {
    return getTimeExactForAlarm(hour, minute, second, 0, weekDays).timeInMillis
}

internal fun Calendar.getTimeExactForAlarmInMilliseconds(
    hour: Int,
    minute: Int,
    second: Int,
    millis: Int,
    weekDays: List<WeekDays>
): Long {
    return getTimeExactForAlarm(hour, minute, second, millis, weekDays).timeInMillis
}

private fun Calendar.getTimeExactForAlarm(
    hour: Int,
    minute: Int,
    second: Int,
    millis: Int,
    weekDays: List<WeekDays>
): Calendar {
    timeInMillis = System.currentTimeMillis()

    set(Calendar.HOUR_OF_DAY, hour)
    set(Calendar.MINUTE, minute)
    set(Calendar.SECOND, second)
    set(Calendar.MILLISECOND, millis)

    val sortedWeekDays = weekDays.sortedBy { it.value }

    when{
        weekDays.isNotEmpty() && !isAlarmForToday(sortedWeekDays, hour, minute) -> setTheDay(sortedWeekDays.getClosestDay())
        weekDays.isEmpty() && !isTimeAhead(hour, minute)-> add(Calendar.DATE, 1)
    }

    return this
}

private fun Calendar.setTheDay(nextWeekDay: Int) {
    val todayDayOfWeek = get(Calendar.DAY_OF_WEEK)

    if (todayDayOfWeek < nextWeekDay) {
        set(Calendar.DAY_OF_WEEK, nextWeekDay)
        return
    }

    if (VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val temporalDayOfWeek = when (nextWeekDay) {
            1 -> DayOfWeek.SUNDAY
            2 -> DayOfWeek.MONDAY
            3 -> DayOfWeek.TUESDAY
            4 -> DayOfWeek.WEDNESDAY
            5 -> DayOfWeek.THURSDAY
            6 -> DayOfWeek.FRIDAY
            7 -> DayOfWeek.SATURDAY
            else -> null
        }

        if (temporalDayOfWeek == null) {
            SmplrAlarmLoggerHolder.logger.e("SmplrAlarm -> The day of week could not be set!")
            return
        }

        val localDate = this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            .with(TemporalAdjusters.next(temporalDayOfWeek))
        set(Calendar.DAY_OF_YEAR, localDate.dayOfYear)
    } else {
        val date = get(Calendar.DAY_OF_MONTH)
        val day = get(Calendar.DAY_OF_WEEK)

        val daysToPostpone = if ((nextWeekDay + 7 - day) % 7 == 0) 7 else (nextWeekDay + 7 - day) % 7
        set(Calendar.DAY_OF_MONTH, date + daysToPostpone)
    }
}

private fun isTimeAhead(hour: Int, minute: Int) = Calendar.getInstance().let {
    it.get(Calendar.HOUR_OF_DAY) < hour
            || (it.get(Calendar.HOUR_OF_DAY) == hour &&
            it.get(Calendar.MINUTE) < minute)
}

private fun List<WeekDays>.getClosestDay(): Int =
    this.map { it.value }
        .firstOrNull {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            it > today
        }
        ?: this.first().value

private fun isAlarmForToday(weekDays: List<WeekDays>, hour: Int, minute: Int): Boolean = Calendar.getInstance().let {
    weekDays.map { weekDay -> weekDay.value }.contains(it.get(Calendar.DAY_OF_WEEK)) && isTimeAhead(hour, minute)
}



