package de.coldtea.smplr.smplralarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.coldtea.smplr.smplralarm.extensions.showNotification
import de.coldtea.smplr.smplralarm.models.SmplrAlarmLoggerHolder
import de.coldtea.smplr.smplralarm.models.launchIo
import de.coldtea.smplr.smplralarm.repository.RoomAlarmStore
import de.coldtea.smplr.smplralarm.services.AlarmSchedulerImpl
import de.coldtea.smplr.smplralarm.services.AlarmService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestId =
            intent.getIntExtra(SmplrAlarmReceiverObjects.SMPLR_ALARM_RECEIVER_INTENT_ID, -1)

        onAlarmReceived(context, requestId)
    }
    private fun onAlarmReceived(context: Context, requestId: Int) {
        runCatching {
            SmplrAlarmLoggerHolder.logger.v("onReceive --> $requestId")

            if (requestId == -1) return

            val store = RoomAlarmStore(context)
            val scheduler = AlarmSchedulerImpl(AlarmService(context), store)

            launchIo {
                runCatching {
                    val definition = store.get(requestId) ?: return@launchIo

                    val config = definition.notificationConfig
                    val channel = config?.channel
                    val notification = config?.notification

                    val contentIntent = config?.contentTarget?.toIntent(context)
                    val fullScreenIntent = config?.fullScreenTarget?.toIntent(context)
                    val alarmReceivedIntent = config?.alarmReceivedTarget?.toIntent(context)

                    if (channel != null && notification != null) {
                        context.showNotification(
                            requestId = requestId,
                            notificationChannelItem = channel,
                            notificationItem = notification,
                            contentIntent = contentIntent,
                            alarmReceivedIntent = alarmReceivedIntent,
                            fullScreenIntent = fullScreenIntent,
                        )
                    }

                    if (definition.weekdays.isEmpty()) {
                        // One-shot alarm: mark inactive and cancel.
                        store.update(definition.copy(isActive = false))
                        scheduler.cancel(definition.id)
                    } else {
                        // Repeating alarm: schedule for the next day.
                        scheduler.rescheduleTomorrow(definition)
                    }
                }.onFailure { throwable ->
                    SmplrAlarmLoggerHolder.logger.e("updateRepeatingAlarm failed: ${throwable.message}", throwable)
                }
            }
        }.onFailure { throwable ->
            SmplrAlarmLoggerHolder.logger.e("onReceive failed: ${throwable.message}", throwable)
        }
    }

    private fun Calendar.dateTime(): Pair<String, String> {
        val sdfDate = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
        val sdfTime = SimpleDateFormat("hh:mm:ss", Locale.getDefault())

        return sdfDate.format(time) to sdfTime.format(time)
    }

    companion object {
        private const val ALARM_RECEIVER_SUCCESS = "Alarm receiver worked successfully"

        fun build(context: Context): Intent {
            return Intent(context, AlarmReceiver::class.java)
        }
    }
}