package de.coldtea.smplr.smplralarm

import de.coldtea.smplr.smplralarm.extensions.getTimeExactForAlarmInMilliseconds
import de.coldtea.smplr.smplralarm.models.WeekDays
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TimeHelpersTest {

    @Test
    fun secondDifference_isOneSecond() {
        val now = Calendar.getInstance()
        val futureHour = (now.get(Calendar.HOUR_OF_DAY) + 1) % 24
        val minute = 0

        val base = Calendar.getInstance()
            .getTimeExactForAlarmInMilliseconds(
                hour = futureHour,
                minute = minute,
                second = 10,
                millis = 0,
                weekDays = emptyList(),
            )

        val plusOneSecond = Calendar.getInstance()
            .getTimeExactForAlarmInMilliseconds(
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

        val base = Calendar.getInstance()
            .getTimeExactForAlarmInMilliseconds(
                hour = futureHour,
                minute = minute,
                second = 10,
                millis = 100,
                weekDays = emptyList(),
            )

        val plusOneMillis = Calendar.getInstance()
            .getTimeExactForAlarmInMilliseconds(
                hour = futureHour,
                minute = minute,
                second = 10,
                millis = 101,
                weekDays = emptyList(),
            )

        val diff = plusOneMillis - base
        assertEquals(1L, diff)
    }
}
