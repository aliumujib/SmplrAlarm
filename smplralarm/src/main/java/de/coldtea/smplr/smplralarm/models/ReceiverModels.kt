package de.coldtea.smplr.smplralarm.receivers

import android.content.Context
import android.content.Intent
import de.coldtea.smplr.smplralarm.models.NotificationChannelItem
import de.coldtea.smplr.smplralarm.models.NotificationItem
import de.coldtea.smplr.smplralarm.models.NotificationTargetDescriptor
import de.coldtea.smplr.smplralarm.models.WeekDays

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal class SmplrAlarmReceiverObjects {
    companion object {
        internal const val SMPLR_ALARM_RECEIVER_INTENT_ID = "smplr_alarm_receiver_intent_id"

        internal var alarmNotification: MutableList<AlarmNotification> = mutableListOf()
    }
}

data class AlarmNotification(
    val alarmNotificationId: Int,
    val hour: Int,
    val min: Int,
    val weekDays: List<WeekDays>,
    val notificationChannelItem: NotificationChannelItem?,
    val notificationItem: NotificationItem?,
    val contentIntent: Intent?,
    val fullScreenIntent: Intent?,
    val alarmReceivedIntent: Intent?,
    val isActive: Boolean,
    val infoPairs: String
)

internal fun NotificationTargetDescriptor.toIntent(context: Context): Intent {
    val intent = when (this) {
        is NotificationTargetDescriptor.ScreenTarget ->
            Intent().setClassName(packageName, activityClassName)
        is NotificationTargetDescriptor.ServiceTarget ->
            Intent().setClassName(packageName, serviceClassName)
        is NotificationTargetDescriptor.BroadcastTarget ->
            Intent().setClassName(packageName, receiverClassName)
    }

    if (action != null) {
        intent.action = action
    }

    extras.forEach { (key, value) ->
        intent.putExtra(key, value)
    }

    return intent
}