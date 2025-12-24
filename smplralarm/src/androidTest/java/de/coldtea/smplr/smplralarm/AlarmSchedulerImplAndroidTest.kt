package de.coldtea.smplr.smplralarm

import android.app.PendingIntent
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.coldtea.smplr.smplralarm.models.AlarmDefinition
import de.coldtea.smplr.smplralarm.models.AlarmStore
import de.coldtea.smplr.smplralarm.models.DefaultAlarmTimeCalculator
import de.coldtea.smplr.smplralarm.models.WeekDays
import de.coldtea.smplr.smplralarm.services.AlarmSchedulerImpl
import de.coldtea.smplr.smplralarm.services.AlarmService
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

private class RecordingAlarmService(context: Context) : AlarmService(context, DefaultAlarmTimeCalculator()) {
    data class Call(
        val requestCode: Int,
        val hour: Int,
        val min: Int,
        val weekDays: List<WeekDays>,
        val second: Int,
        val millis: Int,
    )

    var lastCall: Call? = null

    override fun setAlarm(
        requestCode: Int,
        hour: Int,
        min: Int,
        weekDays: List<WeekDays>,
        second: Int,
        millis: Int,
        receiverIntent: PendingIntent?,
    ) {
        lastCall = Call(requestCode, hour, min, weekDays, second, millis)
    }
}

private object NoopAlarmStoreAndroid : AlarmStore {
    override suspend fun insert(definition: AlarmDefinition) {}
    override suspend fun update(definition: AlarmDefinition) {}
    override suspend fun delete(id: Int) {}
    override suspend fun get(id: Int): AlarmDefinition? = null
    override suspend fun getAll(): List<AlarmDefinition> = emptyList()
}

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerImplAndroidTest {

    @Test
    fun schedule_passesAllTimeComponentsToAlarmService() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val recordingService = RecordingAlarmService(context)
        val scheduler = AlarmSchedulerImpl(recordingService, NoopAlarmStoreAndroid,
            DefaultAlarmTimeCalculator())

        val id = 42
        val hour = 7
        val minute = 15
        val second = 30
        val weekdays = listOf(WeekDays.MONDAY, WeekDays.WEDNESDAY)

        scheduler.schedule(id, hour, minute, second, weekdays)

        val call = recordingService.lastCall
        requireNotNull(call)

        assertEquals(id, call.requestCode)
        assertEquals(hour, call.hour)
        assertEquals(minute, call.min)
        assertEquals(second, call.second)
        assertEquals(weekdays, call.weekDays)
    }
}
