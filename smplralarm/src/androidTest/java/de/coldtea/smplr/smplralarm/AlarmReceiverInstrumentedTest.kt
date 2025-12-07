package de.coldtea.smplr.smplralarm

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.coldtea.smplr.smplralarm.models.AlarmDefinition
import de.coldtea.smplr.smplralarm.models.NotificationConfig
import de.coldtea.smplr.smplralarm.models.WeekDays
import de.coldtea.smplr.smplralarm.receivers.AlarmReceiver
import de.coldtea.smplr.smplralarm.receivers.SmplrAlarmReceiverObjects
import de.coldtea.smplr.smplralarm.repository.RoomAlarmStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmReceiverInstrumentedTest {

    @Test
    fun oneShotAlarm_isMarkedInactiveAfterFire() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = RoomAlarmStore(context)

        val id = 56789
        val def = AlarmDefinition(
            id = id,
            hour = 10,
            minute = 0,
            second = 0,
            millis = 0,
            weekdays = emptyList(), // one-shot
            isActive = true,
            nextTriggerTime = null,
            metadata = emptyMap(),
            notificationConfig = NotificationConfig(
                channel = null,
                notification = null,
            ),
        )

        store.insert(def)

        val intent = AlarmReceiver.build(context).apply {
            putExtra(SmplrAlarmReceiverObjects.SMPLR_ALARM_RECEIVER_INTENT_ID, id)
        }

        AlarmReceiver().onReceive(context, intent)

        // AlarmReceiver updates the store on a background coroutine; give it
        // a short window to complete before asserting.
        delay(500)

        val updated = store.get(id)!!
        assertFalse("One-shot alarm should be inactive after firing", updated.isActive)
    }

    @Test
    fun repeatingAlarm_staysActiveAfterFire() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = RoomAlarmStore(context)

        val id = 67890
        val def = AlarmDefinition(
            id = id,
            hour = 11,
            minute = 30,
            second = 0,
            millis = 0,
            weekdays = listOf(WeekDays.MONDAY), // repeating
            isActive = true,
            nextTriggerTime = null,
            metadata = emptyMap(),
            notificationConfig = NotificationConfig(
                channel = null,
                notification = null,
            ),
        )

        store.insert(def)

        val intent = AlarmReceiver.build(context).apply {
            putExtra(SmplrAlarmReceiverObjects.SMPLR_ALARM_RECEIVER_INTENT_ID, id)
        }

        AlarmReceiver().onReceive(context, intent)

        val updated = store.get(id)!!
        assertTrue("Repeating alarm should stay active after firing", updated.isActive)
    }
}
