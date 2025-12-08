package de.coldtea.smplr.smplralarm.extensions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import de.coldtea.smplr.smplralarm.R
import de.coldtea.smplr.smplralarm.SmplrAlarmEnvironment
import de.coldtea.smplr.smplralarm.apis.SmplrAlarmAPI.Companion.SMPLR_ALARM_NOTIFICATION_ID
import de.coldtea.smplr.smplralarm.apis.SmplrAlarmAPI.Companion.SMPLR_ALARM_REQUEST_ID
import de.coldtea.smplr.smplralarm.models.NotificationChannelItem
import de.coldtea.smplr.smplralarm.models.NotificationItem
import de.coldtea.smplr.smplralarm.models.toIntent

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

private fun Context.createNotificationChannelIfNotExists(notificationChannelItem: NotificationChannelItem) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val notificationManager =
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val config = SmplrAlarmEnvironment.current(this)
    val logger = config.logger

    // Notification channels are idempotent, calling this multiple times is safe.
    if (notificationManager.getNotificationChannel(notificationChannelItem.channelId) == null) {
        with(notificationChannelItem) {
            logger.d("Creating notification channel: ID=$channelId, Name=${name.takeIf { it.isNotEmpty() } ?: packageName}, Importance=$importance")

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
    fullScreenIntent: Intent? = null
) {
    val config = SmplrAlarmEnvironment.current(this)
    config.logger.d("Creating notification: ID=$requestId, Channel ID=${notificationChannelItem.channelId}, Title='${notificationItem.title}' ${notificationItem.smallIcon}")

    runCatching {
        createNotificationChannelIfNotExists(notificationChannelItem)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

                    notificationItem.dismissTarget?.let { target ->
                        val intent = target.toIntent(this@showNotification)
                        setDeleteIntent(
                            this@showNotification.getBroadcast(
                                requestId,
                                intent
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

                    notificationItem.firstButtonText?.let { text ->
                        notificationItem.firstButtonTarget?.let { target ->
                            val intent = target.toIntent(this@showNotification)
                            addAction(
                                0,
                                text,
                                this@showNotification.getBroadcast(
                                    requestId,
                                    intent,
                                )
                            )
                        }
                    }

                    notificationItem.secondButtonText?.let { text ->
                        notificationItem.secondButtonTarget?.let { target ->
                            val intent = target.toIntent(this@showNotification)
                            addAction(
                                0,
                                text,
                                this@showNotification.getBroadcast(
                                    requestId,
                                    intent,
                                )
                            )
                        }
                    }
                }
            }.build()

        notificationManager.notify(requestId, notification)
    }.onFailure { exception ->
        config.logger.e("Failed to create notification ${exception.stackTraceToString()}", exception)
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