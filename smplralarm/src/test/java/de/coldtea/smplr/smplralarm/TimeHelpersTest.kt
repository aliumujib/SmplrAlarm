package de.coldtea.smplr.smplralarm

import de.coldtea.smplr.smplralarm.models.DefaultAlarmTimeCalculator
import de.coldtea.smplr.smplralarm.models.WeekDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar

class TimeHelpersTest {

    private val calculator = DefaultAlarmTimeCalculator()
    private val utcZone = ZoneId.of("UTC")

    @Test
    fun secondDifference_isOneSecond() {
        val now = Calendar.getInstance()
        val futureHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
        val minute = 0

        val base = calculator.computeNextTriggerTimeMillis(
            hour = futureHour,
            minute = minute,
            second = 10,
            millis = 0,
            weekDays = emptyList(),
        )

        val plusOneSecond = calculator.computeNextTriggerTimeMillis(
            hour = futureHour,
            minute = minute,
            second = 11,
            millis = 0,
            weekDays = emptyList(),
        )

        val diff = plusOneSecond - base
        assertEquals(1000L, diff)
    }

    @Test
    fun millisDifference_isOneMillisecond() {
        val now = Calendar.getInstance()
        val futureHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
        val minute = 0

        val base = calculator.computeNextTriggerTimeMillis(
            hour = futureHour,
            minute = minute,
            second = 10,
            millis = 100,
            weekDays = emptyList(),
        )

        val plusOneMillis = calculator.computeNextTriggerTimeMillis(
            hour = futureHour,
            minute = minute,
            second = 10,
            millis = 101,
            weekDays = emptyList(),
        )

        val diff = plusOneMillis - base
        assertEquals(1L, diff)
    }

    /**
     * Regression test for single-day recurring alarm bug.
     *
     * Scenario: It's Sunday at 15:51, and we set an alarm for 15:00 on Sundays only.
     * Expected: The alarm should be scheduled for NEXT Sunday (7 days later), not today.
     * Bug: Previously, the fallback loop would reset to today and return today's date,
     *      causing the alarm to fire immediately since 15:00 has already passed.
     */
    @Test
    fun singleDayRecurringAlarm_withPassedTime_schedulesForNextWeek() {
        // Given: Sunday Feb 22, 2026 at 15:51:00 UTC
        val sundayAt1551 = ZonedDateTime.of(2026, 2, 22, 15, 51, 0, 0, utcZone)
        val fixedClock = Clock.fixed(sundayAt1551.toInstant(), utcZone)
        val testCalculator = DefaultAlarmTimeCalculator(utcZone, fixedClock)

        // When: Calculate next trigger for 15:00 on Sundays only
        val nextTriggerMillis = testCalculator.computeNextTriggerTimeMillis(
            hour = 15,
            minute = 0,
            second = 0,
            millis = 0,
            weekDays = listOf(WeekDays.SUNDAY),
        )

        // Then: Should be next Sunday (March 1, 2026), not today
        val nextTriggerDate = Instant.ofEpochMilli(nextTriggerMillis)
            .atZone(utcZone)
            .toLocalDate()

        // Next Sunday is March 1, 2026
        val expectedDate = LocalDate.of(2026, 3, 1)
        assertEquals(expectedDate, nextTriggerDate)

        // Also verify the trigger time is in the future
        assertTrue(
            "Next trigger should be after now",
            nextTriggerMillis > sundayAt1551.toInstant().toEpochMilli()
        )
    }

    /**
     * Test that multi-day recurring alarms still work correctly.
     *
     * Scenario: It's Sunday at 15:51, alarm for 9:00 on all weekdays.
     * Expected: Should schedule for Monday 9:00.
     */
    @Test
    fun multiDayRecurringAlarm_withPassedTime_schedulesForNextValidDay() {
        // Given: Sunday Feb 22, 2026 at 15:51:00 UTC
        val sundayAt1551 = ZonedDateTime.of(2026, 2, 22, 15, 51, 0, 0, utcZone)
        val fixedClock = Clock.fixed(sundayAt1551.toInstant(), utcZone)
        val testCalculator = DefaultAlarmTimeCalculator(utcZone, fixedClock)

        // When: Calculate next trigger for 9:00 on all days
        val nextTriggerMillis = testCalculator.computeNextTriggerTimeMillis(
            hour = 9,
            minute = 0,
            second = 0,
            millis = 0,
            weekDays = listOf(
                WeekDays.MONDAY,
                WeekDays.TUESDAY,
                WeekDays.WEDNESDAY,
                WeekDays.THURSDAY,
                WeekDays.FRIDAY,
                WeekDays.SATURDAY,
                WeekDays.SUNDAY,
            ),
        )

        // Then: Should be Monday Feb 23, 2026 at 9:00
        val nextTrigger = Instant.ofEpochMilli(nextTriggerMillis).atZone(utcZone)
        val expectedDate = LocalDate.of(2026, 2, 23)
        assertEquals(expectedDate, nextTrigger.toLocalDate())
        assertEquals(9, nextTrigger.hour)
    }

    /**
     * Test that same-day alarms work when time hasn't passed yet.
     *
     * Scenario: It's Sunday at 14:00, alarm for 15:00 on Sundays.
     * Expected: Should schedule for today at 15:00.
     */
    @Test
    fun singleDayRecurringAlarm_withFutureTime_schedulesForToday() {
        // Given: Sunday Feb 22, 2026 at 14:00:00 UTC
        val sundayAt1400 = ZonedDateTime.of(2026, 2, 22, 14, 0, 0, 0, utcZone)
        val fixedClock = Clock.fixed(sundayAt1400.toInstant(), utcZone)
        val testCalculator = DefaultAlarmTimeCalculator(utcZone, fixedClock)

        // When: Calculate next trigger for 15:00 on Sundays only
        val nextTriggerMillis = testCalculator.computeNextTriggerTimeMillis(
            hour = 15,
            minute = 0,
            second = 0,
            millis = 0,
            weekDays = listOf(WeekDays.SUNDAY),
        )

        // Then: Should be today (Sunday Feb 22, 2026) at 15:00
        val nextTrigger = Instant.ofEpochMilli(nextTriggerMillis).atZone(utcZone)
        val expectedDate = LocalDate.of(2026, 2, 22)
        assertEquals(expectedDate, nextTrigger.toLocalDate())
        assertEquals(15, nextTrigger.hour)
    }
}
