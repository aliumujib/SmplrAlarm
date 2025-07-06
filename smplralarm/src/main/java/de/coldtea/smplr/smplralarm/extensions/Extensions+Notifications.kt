package de.coldtea.smplr.smplralarm.extensions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import de.coldtea.smplr.smplralarm.R
import de.coldtea.smplr.smplralarm.apis.SmplrAlarmAPI.Companion.SMPLR_ALARM_NOTIFICATION_ID
import de.coldtea.smplr.smplralarm.apis.SmplrAlarmAPI.Companion.SMPLR_ALARM_REQUEST_ID
import de.coldtea.smplr.smplralarm.models.NotificationChannelItem
import de.coldtea.smplr.smplralarm.models.NotificationItem
import timber.log.Timber

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

private fun Context.createNotificationChannelIfNotExists(notificationChannelItem: NotificationChannelItem) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Notification channels are idempotent, calling this multiple times is safe.
    if (notificationManager.getNotificationChannel(notificationChannelItem.channelId) == null) {
        with(notificationChannelItem) {
            Timber.d("Creating notification channel: ID=$channelId, Name=${name.takeIf { it.isNotEmpty() } ?: packageName}, Importance=$importance")

            val channel = NotificationChannel(
                channelId,
                name.takeIf { it.isNotEmpty() } ?: packageName,
                importance).apply {
                description = this@with.description
                setShowBadge(showBadge)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}

internal fun Context.showNotification(
    requestId: Int,
    notificationChannelItem: NotificationChannelItem,
    notificationItem: NotificationItem,
    contentIntent: Intent? = null,
    alarmReceivedIntent: Intent? = null,
    fullScreenIntent: Intent? = null
) {
    runCatching {
        createNotificationChannelIfNotExists(notificationChannelItem)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        Timber.d("Creating notification: ID=$requestId, Channel ID=${notificationChannelItem.channelId}, Title='${notificationItem.title}' ${notificationItem.smallIcon}")

        val notification =
            NotificationCompat.Builder(this, notificationChannelItem.channelId).apply {
                priority = NotificationCompat.PRIORITY_HIGH
                with(notificationItem) {
                    setSmallIcon(smallIcon.takeIf { it != 0 } ?: R.drawable.ic_baseline_notifications_active_24)
                    setContentTitle(title)
                    setContentText(message)
                    setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                    priority = NotificationCompat.PRIORITY_DEFAULT
                    setAutoCancel(autoCancel != false)
                    setAllowSystemGeneratedContextualActions(false)

                    if (notificationItem.notificationDismissedIntent != null) {
                        setDeleteIntent(
                            this@showNotification.getBroadcast(
                                requestId,
                                requireNotNull(notificationItem.notificationDismissedIntent)
                            )
                        )
                    }

                    if (contentIntent != null) {
                        setContentIntent(
                            getScreenIntent(requestId, contentIntent)
                        )
                    }

                    if (fullScreenIntent != null) {
                        setFullScreenIntent(
                            getScreenIntent(requestId, fullScreenIntent),
                            true
                        )
                    }

                    if (notificationItem.firstButtonText != null && notificationItem.firstButtonIntent != null) addAction(
                        0,
                        notificationItem.firstButtonText,
                        this@showNotification.getBroadcast(
                            requestId,
                            requireNotNull(notificationItem.firstButtonIntent)
                        )
                    )

                    if (notificationItem.secondButtonText != null && notificationItem.secondButtonIntent != null) addAction(
                        0,
                        notificationItem.secondButtonText,
                        this@showNotification.getBroadcast(
                            requestId,
                            requireNotNull(notificationItem.secondButtonIntent)
                        )
                    )
                }
            }.build()

        notificationManager.notify(requestId, notification)
    }.onFailure { exception ->
        Timber.e("Failed to create notification ${exception.stackTraceToString()}")
    }

    if (alarmReceivedIntent != null) {
        alarmReceivedIntent.putExtra(SMPLR_ALARM_REQUEST_ID, requestId)
        sendBroadcast(alarmReceivedIntent)
    }
}

internal fun Context.getScreenIntent(requestId: Int, intent: Intent): PendingIntent =
    PendingIntent.getActivity(
        this,
        requestId,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )

private fun Context.getBroadcast(requestId: Int, intent: Intent): PendingIntent =
    PendingIntent.getBroadcast(
        this,
        requestId,
        intent.apply {
            putExtra(SMPLR_ALARM_NOTIFICATION_ID, requestId)
        },
        PendingIntent.FLAG_IMMUTABLE
    )