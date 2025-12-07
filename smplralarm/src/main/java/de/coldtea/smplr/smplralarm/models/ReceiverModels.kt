package de.coldtea.smplr.smplralarm.models

import android.content.Context
import android.content.Intent

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal class SmplrAlarmReceiverObjects {
    companion object {
        internal const val SMPLR_ALARM_RECEIVER_INTENT_ID = "smplr_alarm_receiver_intent_id"
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

 fun screenTargetFromIntent(
    intent: Intent
): NotificationTargetDescriptor.ScreenTarget {
    val component = requireNotNull(intent.component) {
        "Intent must have an explicit component to build a ScreenTarget"
    }

    val extras = intent.extras?.let { bundle ->
        val result = mutableMapOf<String, String>()
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            if (value != null) result[key] = value.toString()
        }
        result.toMap()
    } ?: emptyMap()

    return NotificationTargetDescriptor.ScreenTarget(
        packageName = component.packageName,
        activityClassName = component.className,
        action = intent.action,
        extras = extras,
    )
}

 fun serviceTargetFromIntent(
    intent: Intent
): NotificationTargetDescriptor.ServiceTarget {
    val component = requireNotNull(intent.component) {
        "Intent must have an explicit component to build a ServiceTarget"
    }

    val extras = intent.extras?.let { bundle ->
        val result = mutableMapOf<String, String>()
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            if (value != null) result[key] = value.toString()
        }
        result.toMap()
    } ?: emptyMap()

    return NotificationTargetDescriptor.ServiceTarget(
        packageName = component.packageName,
        serviceClassName = component.className,
        action = intent.action,
        extras = extras,
    )
}

 fun broadcastTargetFromIntent(
    intent: Intent
): NotificationTargetDescriptor.BroadcastTarget {
    val component = requireNotNull(intent.component) {
        "Intent must have an explicit component to build a BroadcastTarget"
    }

    val extras = intent.extras?.let { bundle ->
        val result = mutableMapOf<String, String>()
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            if (value != null) result[key] = value.toString()
        }
        result.toMap()
    } ?: emptyMap()

    return NotificationTargetDescriptor.BroadcastTarget(
        packageName = component.packageName,
        receiverClassName = component.className,
        action = intent.action,
        extras = extras,
    )
}