package de.coldtea.smplr.smplralarm.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import de.coldtea.smplr.smplralarm.extensions.getTimeExactForAlarmInMilliseconds
import de.coldtea.smplr.smplralarm.models.WeekDays
import de.coldtea.smplr.smplralarm.models.SmplrAlarmLoggerHolder
import de.coldtea.smplr.smplralarm.receivers.ActivateAppReceiver
import de.coldtea.smplr.smplralarm.receivers.AlarmReceiver
import de.coldtea.smplr.smplralarm.models.SmplrAlarmReceiverObjects
import java.util.*

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */
open class AlarmService(val context: Context) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    private val calendar
        get() = Calendar.getInstance()

    open fun setAlarm(
        requestCode: Int,
        hour: Int,
        min: Int,
        weekDays: List<WeekDays>,
        second: Int = 0,
        millis: Int = 0,
        receiverIntent: PendingIntent? = null
    ) {
        val hasAlarmPermission = if (Build.VERSION.SDK_INT >= (Build.VERSION_CODES.S)) {
            context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
        } else {
            true
        }

        if (!hasAlarmPermission) {
            throw IllegalStateException("setAlarm --> Can not set alarm, permissions missing")
        }

        val alarmReceiverIntent = receiverIntent ?: createReceiverPendingIntent(requestCode, 0)
        val openAppIntent = createOpenAppPendingIntent(requestCode, 0)
        val exactAlarmTime =
            calendar.getTimeExactForAlarmInMilliseconds(hour, min, second, millis, weekDays)

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            exactAlarmTime,
            openAppIntent
        )

        alarmManager.setAlarmClock(
            alarmClockInfo,
            alarmReceiverIntent
        )
    }

    fun alarmExist(requestCode: Int): Boolean =
        getReceiverPendingIntent(
            requestCode,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null

    fun cancelAlarm(requestCode: Int) {
        val pendingIntent =
            getReceiverPendingIntent(
                requestCode,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            ) ?: return
        alarmManager.cancel(pendingIntent)
    }

    private fun createReceiverPendingIntent(requestCode: Int, flag: Int) =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(
                context,
                AlarmReceiver::class.java
            ).putExtra(SmplrAlarmReceiverObjects.SMPLR_ALARM_RECEIVER_INTENT_ID, requestCode),
            flag or PendingIntent.FLAG_IMMUTABLE
        )

    private fun getReceiverPendingIntent(requestCode: Int, flag: Int) = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, AlarmReceiver::class.java).putExtra(
            SmplrAlarmReceiverObjects.SMPLR_ALARM_RECEIVER_INTENT_ID,
            requestCode
        ),
        flag or PendingIntent.FLAG_IMMUTABLE
    )

    private fun createOpenAppPendingIntent(requestCode: Int, flag: Int) =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(
                context,
                ActivateAppReceiver::class.java
            ).putExtra(
                SmplrAlarmReceiverObjects.SMPLR_ALARM_RECEIVER_INTENT_ID,
                requestCode
            ),
            flag or PendingIntent.FLAG_IMMUTABLE
        )
}