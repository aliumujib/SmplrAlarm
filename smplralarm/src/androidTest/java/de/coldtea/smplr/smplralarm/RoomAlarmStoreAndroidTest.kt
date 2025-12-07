package de.coldtea.smplr.smplralarm

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.coldtea.smplr.smplralarm.models.AlarmDefinition
import de.coldtea.smplr.smplralarm.models.NotificationConfig
import de.coldtea.smplr.smplralarm.models.WeekDays
import de.coldtea.smplr.smplralarm.repository.RoomAlarmStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomAlarmStoreAndroidTest {

    @Test
    fun insertAndGet_preservesSecondAndMillis() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = RoomAlarmStore(context)

        val def = AlarmDefinition(
            id = 0,
            hour = 9,
            minute = 45,
            second = 12,
            millis = 345,
            weekdays = listOf(WeekDays.TUESDAY, WeekDays.THURSDAY),
            isActive = true,
            nextTriggerTime = 123456789L,
            metadata = mapOf("k" to "v"),
            notificationConfig = NotificationConfig(
                channel = null,
                notification = null,
            ),
        )

        store.insert(def)
        val all = store.getAll()
        val stored = all.maxByOrNull { it.id }!!

        assertEquals(def.hour, stored.hour)
        assertEquals(def.minute, stored.minute)
        assertEquals(def.second, stored.second)
        assertEquals(def.millis, stored.millis)
    }
}
