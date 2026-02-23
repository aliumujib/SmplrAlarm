package de.coldtea.smplr.smplralarm.models

import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Default implementation of [AlarmTimeCalculator] using java.time APIs.
 *
 * This is designed to be behaviorally equivalent to the existing
 * Calendar-based helpers, but implemented with modern time types.
 *
 * @param zoneId The time zone to use for calculations. Defaults to system default.
 * @param clock The clock to use for getting current time. Defaults to system clock.
 *              This parameter is primarily for testing purposes.
 */
class DefaultAlarmTimeCalculator(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.systemDefaultZone(),
) : AlarmTimeCalculator {

    override fun computeNextTriggerTimeMillis(
        hour: Int,
        minute: Int,
        second: Int,
        millis: Int,
        weekDays: List<WeekDays>,
    ): Long {
        val nowInstant = clock.instant()
        val now = nowInstant.atZone(zoneId)

        val targetTime = LocalTime.of(hour, minute, second, millis * 1_000_000)

        val nextDate = if (weekDays.isEmpty()) {
            computeNextDateForOneShot(now.toLocalDate(), targetTime, nowInstant)
        } else {
            computeNextDateForRepeating(now.toLocalDate(), targetTime, nowInstant, weekDays)
        }

        return nextDate
            .atTime(targetTime)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun computeNextDateForOneShot(
        today: LocalDate,
        targetTime: LocalTime,
        nowInstant: Instant,
    ): LocalDate {
        val candidateToday = today.atTime(targetTime).atZone(zoneId).toInstant()
        return if (candidateToday.isAfter(nowInstant)) {
            today
        } else {
            today.plusDays(1)
        }
    }

    private fun computeNextDateForRepeating(
        today: LocalDate,
        targetTime: LocalTime,
        nowInstant: Instant,
        weekDays: List<WeekDays>,
    ): LocalDate {
        val allowed = weekDays.map { it.toDayOfWeek() }.toSet()

        var date = today
        repeat(7) {
            if (date.dayOfWeek in allowed) {
                val candidateInstant = date
                    .atTime(targetTime)
                    .atZone(zoneId)
                    .toInstant()
                if (candidateInstant.isAfter(nowInstant)) {
                    return date
                }
            }
            date = date.plusDays(1)
        }

        // Fallback: continue from where we left off (date is now today+7).
        // Don't reset to today - that would return a past date for single-day schedules
        // where the time has already passed.
        while (true) {
            if (date.dayOfWeek in allowed) return date
            date = date.plusDays(1)
        }
    }
}

private fun WeekDays.toDayOfWeek(): DayOfWeek = when (this.value) {
    java.util.Calendar.SUNDAY -> DayOfWeek.SUNDAY
    java.util.Calendar.MONDAY -> DayOfWeek.MONDAY
    java.util.Calendar.TUESDAY -> DayOfWeek.TUESDAY
    java.util.Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
    java.util.Calendar.THURSDAY -> DayOfWeek.THURSDAY
    java.util.Calendar.FRIDAY -> DayOfWeek.FRIDAY
    java.util.Calendar.SATURDAY -> DayOfWeek.SATURDAY
    else -> DayOfWeek.MONDAY
}
