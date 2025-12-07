package de.coldtea.smplr.smplralarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.coldtea.smplr.smplralarm.SmplrAlarmEnvironment
import de.coldtea.smplr.smplralarm.extensions.showNotification
import de.coldtea.smplr.smplralarm.models.SmplrAlarmReceiverObjects
import de.coldtea.smplr.smplralarm.models.launchIo
import de.coldtea.smplr.smplralarm.models.toIntent

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
        val config = SmplrAlarmEnvironment.current(context)
        val store = config.storeFactory(context)
        val scheduler = config.schedulerFactory(context)
        val logger = config.logger
        logger.v("onReceive --> $requestId")

        runCatching {
            if (requestId == -1) throw IllegalArgumentException("onAlarmReceived: alarm requestId $requestId is invalid")

            launchIo {
                runCatching {
                    val definition = store.get(requestId) ?: throw IllegalArgumentException("onAlarmReceived: alarm with requestId $requestId is not found")

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
                    } else {
                        logger.v("onAlarmReceived: skipping notification as notification config is not available $channel $notification")
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
                    logger.e(
                        "updateRepeatingAlarm failed: ${throwable.message}",
                        throwable
                    )
                }
            }
        }.onFailure { throwable ->
            logger.e("onReceive failed: ${throwable.message}", throwable)
        }
    }

    companion object {
        private const val ALARM_RECEIVER_SUCCESS = "Alarm receiver worked successfully"

        fun build(context: Context): Intent {
            return Intent(context, AlarmReceiver::class.java)
        }
    }
}