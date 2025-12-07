package de.coldtea.smplr.smplralarm

import android.content.Context
import de.coldtea.smplr.smplralarm.apis.AlarmNotificationAPI
import de.coldtea.smplr.smplralarm.apis.ChannelManagerAPI
import de.coldtea.smplr.smplralarm.apis.SmplrAlarmAPI
import de.coldtea.smplr.smplralarm.models.NotificationChannelItem
import de.coldtea.smplr.smplralarm.models.NotificationItem

/**
 * SmplrAlarm Library, Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 *
 * SmplrAlarm is a convenience library to create alarms way simpler than default way.
 * Main goal of this library is providing a clean, simple and convenient API to manage alarms.
 *
 * MIT License
 * Copyright (c) 2020 Yasar Naci Gündüz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

/**
 * Environment-backed DSL entry point for setting an alarm.
 *
 * This uses [SmplrAlarmEnvironment] via [SmplrAlarmAPI]’s convenience
 * constructor. Configure the environment once at app startup to change
 * store/scheduler/logger/id generator globally.
 */
suspend fun smplrAlarmSet(
    context: Context,
    lambda: SmplrAlarmAPI.() -> Unit,
): Int = SmplrAlarmAPI(context).apply(lambda).setAlarm()

/**
 * Environment-backed DSL for cancelling an alarm.
 */
suspend fun smplrAlarmCancel(
    context: Context,
    lambda: SmplrAlarmAPI.() -> Unit,
) = SmplrAlarmAPI(context).apply(lambda).removeAlarm()

/**
 * Environment-backed DSL for renewing missing alarms.
 */
suspend fun smplrAlarmRenewMissingAlarms(
    context: Context,
) = SmplrAlarmAPI(context).renewMissingAlarms()

/**
 * Environment-backed DSL for updating an alarm.
 */
suspend fun smplrAlarmUpdate(
    context: Context,
    lambda: SmplrAlarmAPI.() -> Unit,
) = SmplrAlarmAPI(context).apply(lambda).updateAlarm()

/**
 * Data item which holds the following information that accapted and/or required by Android Notification channel
 *
 * importance,showBadge , name, description
 */
fun channel(lambda: ChannelManagerAPI.() -> Unit): NotificationChannelItem =
    ChannelManagerAPI().apply(lambda).build()

/**
 * Data item which holds the following information that accapted and/or required by Android Notification
 *
 * smallIcon, title, message, bigText, autoCancel
 *
 * Additionally holds the following arguments to insert buttons and respective intents which is executed at the click
 *
 * - firstButtonText
 * - secondButtonText
 * - firstButtonIntent
 * - secondButtonIntent
 * - notificationDismissedIntent: The action intent which is executed when the notification is dismissed.
 */
fun alarmNotification(lamda: AlarmNotificationAPI.() -> Unit): NotificationItem =
    AlarmNotificationAPI().apply(lamda).build()