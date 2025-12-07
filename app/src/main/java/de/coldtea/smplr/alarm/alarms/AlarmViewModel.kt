package de.coldtea.smplr.alarm.alarms

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.coldtea.smplr.alarm.MainActivity
import de.coldtea.smplr.alarm.R
import de.coldtea.smplr.alarm.alarms.models.WeekInfo
import de.coldtea.smplr.alarm.lockscreenalarm.ActivityLockScreenAlarm
import de.coldtea.smplr.alarm.receiver.ActionReceiver
import de.coldtea.smplr.alarm.receiver.AlarmBroadcastReceiver
import de.coldtea.smplr.smplralarm.*
import de.coldtea.smplr.smplralarm.models.NotificationItem
import de.coldtea.smplr.smplralarm.models.broadcastTargetFromIntent
import de.coldtea.smplr.smplralarm.models.screenTargetFromIntent
import de.coldtea.smplr.smplralarm.repository.RoomAlarmStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmViewModel : ViewModel() {

    sealed class AlarmScheduleState {
        object Idle : AlarmScheduleState()
        object Loading : AlarmScheduleState()
        data class Success(val requestCode: Int) : AlarmScheduleState()
        data class Error(val throwable: Throwable) : AlarmScheduleState()
    }

    private val _scheduleState = MutableStateFlow<AlarmScheduleState>(AlarmScheduleState.Idle)
    val scheduleState: StateFlow<AlarmScheduleState> = _scheduleState.asStateFlow()

    fun clearScheduleState() {
        _scheduleState.value = AlarmScheduleState.Idle
    }

    fun setFullScreenIntentAlarm(
        hour: Int,
        minute: Int,
        second: Int,
        millis: Int,
        weekInfo: WeekInfo,
        applicationContext: Context
    ) {
        val onClickShortcutIntent = Intent(
            applicationContext,
            MainActivity::class.java
        )

        val fullScreenIntent = Intent(
            applicationContext,
            ActivityLockScreenAlarm::class.java
        )

        val alarmReceivedIntent = Intent(
            applicationContext,
            AlarmBroadcastReceiver::class.java
        )

        val snoozeIntent = Intent(applicationContext, ActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(HOUR, hour)
            putExtra(MINUTE, minute)
        }

        val dismissIntent = Intent(applicationContext, ActionReceiver::class.java).apply {
            action = ACTION_DISMISS
        }

        val notificationDismissIntent = Intent(applicationContext, ActionReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_DISMISS
        }

        fullScreenIntent.putExtra("SmplrText", "You did it, you crazy bastard you did it!")

        viewModelScope.launch {
            _scheduleState.value = AlarmScheduleState.Loading

            runCatching {
                smplrAlarmSet(applicationContext) {
                    hour { hour }
                    min { minute }
                    second { second }
                    millis { millis }
                    notificationChannel {
                        channel {
                            channelId { "smplr_alarm_fullscreen_channel" }
                            name { "Smplr Alarm Full Screen" }
                            description { "Alarms shown as full-screen intents" }
                            importance { NotificationManager.IMPORTANCE_HIGH }
                            showBadge { false }
                        }
                    }
                    contentTarget { screenTargetFromIntent(onClickShortcutIntent) }
                    contentTarget { screenTargetFromIntent(fullScreenIntent) }
                    alarmReceivedTarget { broadcastTargetFromIntent(alarmReceivedIntent) }
                    weekdays {
                        if (weekInfo.monday) monday()
                        if (weekInfo.tuesday) tuesday()
                        if (weekInfo.wednesday) wednesday()
                        if (weekInfo.thursday) thursday()
                        if (weekInfo.friday) friday()
                        if (weekInfo.saturday) saturday()
                        if (weekInfo.sunday) sunday()
                    }
                    notification {
                        alarmNotification {
                            smallIcon { R.drawable.ic_baseline_alarm_on_24 }
                            title { "Simple alarm is ringing" }
                            message { "Simple alarm is ringing" }
                            bigText { "Simple alarm is ringing" }
                            autoCancel { true }
                            firstButtonText { "Snooze" }
                            secondButtonText { "Dismiss" }
                            firstButtonTarget { broadcastTargetFromIntent(snoozeIntent) }
                            secondButtonTarget { broadcastTargetFromIntent(dismissIntent) }
                            dismissTarget { broadcastTargetFromIntent(notificationDismissIntent) }
                        }
                    }
                    infoPairs {
                        listOf(
                            "a" to "b",
                            "b" to "c",
                            "c" to "d"
                        )
                    }
                }
            }.onSuccess { requestCode ->
                _scheduleState.value = AlarmScheduleState.Success(requestCode)
            }.onFailure { throwable ->
                _scheduleState.value = AlarmScheduleState.Error(throwable)
            }
        }
    }

    fun setNotificationAlarm(
        hour: Int,
        minute: Int,
        second: Int,
        millis: Int,
        weekInfo: WeekInfo,
        applicationContext: Context
    ) {
        val alarmReceivedIntent = Intent(
            applicationContext,
            AlarmBroadcastReceiver::class.java
        )
        val onClickShortcutIntent = Intent(
            applicationContext,
            MainActivity::class.java
        )

        onClickShortcutIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP

        viewModelScope.launch {
            _scheduleState.value = AlarmScheduleState.Loading

            runCatching {
                smplrAlarmSet(applicationContext) {
                    hour { hour }
                    min { minute }
                    second { second }
                    millis { millis }
                    weekdays {
                        if (weekInfo.monday) monday()
                        if (weekInfo.tuesday) tuesday()
                        if (weekInfo.wednesday) wednesday()
                        if (weekInfo.thursday) thursday()
                        if (weekInfo.friday) friday()
                        if (weekInfo.saturday) saturday()
                        if (weekInfo.sunday) sunday()
                    }

                    notificationChannel {
                        channel {
                            channelId { "smplr_alarm_notification_channel" }
                            name { "Smplr Alarm Notifications" }
                            description { "Standard SmplrAlarm notifications" }
                            importance { NotificationManager.IMPORTANCE_HIGH }
                            showBadge { true }
                        }
                    }
                    contentTarget { screenTargetFromIntent(onClickShortcutIntent) }
                    alarmReceivedTarget { broadcastTargetFromIntent(alarmReceivedIntent) }
                    notification {
                        alarmNotification {
                            smallIcon { R.drawable.ic_baseline_alarm_on_24 }
                            title { "Simple alarm is ringing" }
                            message { "Simple alarm is ringing" }
                            bigText { "Simple alarm is ringing" }
                            autoCancel { true }
                            firstButtonText { "Snooze" }
                            secondButtonText { "Dismiss" }
                        }
                    }
                }
            }.onSuccess { requestCode ->
                _scheduleState.value = AlarmScheduleState.Success(requestCode)
            }.onFailure { throwable ->
                _scheduleState.value = AlarmScheduleState.Error(throwable)
            }
        }
    }

    fun setNoNotificationAlarm(
        hour: Int,
        minute: Int,
        second: Int,
        millis: Int,
        weekInfo: WeekInfo,
        applicationContext: Context
    ) {

        val fullScreenIntent = Intent(
            applicationContext,
            ActivityLockScreenAlarm::class.java
        )

        val alarmReceivedIntent = Intent(
            applicationContext,
            AlarmBroadcastReceiver::class.java
        )

        fullScreenIntent.putExtra("SmplrText", "You did it, you crazy bastard you did it!")

        viewModelScope.launch {
            _scheduleState.value = AlarmScheduleState.Loading

            runCatching {
                smplrAlarmSet(applicationContext) {
                    hour { hour }
                    min { minute }
                    second { second }
                    millis { millis }
                    contentTarget { screenTargetFromIntent(fullScreenIntent) }
                    alarmReceivedTarget { broadcastTargetFromIntent(alarmReceivedIntent) }
                    weekdays {
                        if (weekInfo.monday) monday()
                        if (weekInfo.tuesday) tuesday()
                        if (weekInfo.wednesday) wednesday()
                        if (weekInfo.thursday) thursday()
                        if (weekInfo.friday) friday()
                        if (weekInfo.saturday) saturday()
                        if (weekInfo.sunday) sunday()
                    }
                    infoPairs {
                        listOf(
                            "a" to "b",
                            "b" to "c",
                            "c" to "d"
                        )
                    }
                }
            }.onSuccess { requestCode ->
                _scheduleState.value = AlarmScheduleState.Success(requestCode)
            }.onFailure { throwable ->
                _scheduleState.value = AlarmScheduleState.Error(throwable)
            }
        }
    }

    fun updateAlarm(
        requestCode: Int,
        hour: Int,
        minute: Int,
        weekInfo: WeekInfo,
        isActive: Boolean,
        applicationContext: Context
    ) {
        viewModelScope.launch {
            smplrAlarmUpdate(applicationContext,) {
                requestCode { requestCode }
                hour { hour }
                min { minute }
                weekdays {
                    if (weekInfo.monday) monday()
                    if (weekInfo.tuesday) tuesday()
                    if (weekInfo.wednesday) wednesday()
                    if (weekInfo.thursday) thursday()
                    if (weekInfo.friday) friday()
                    if (weekInfo.saturday) saturday()
                    if (weekInfo.sunday) sunday()
                }
                isActive { isActive }
            }
        }
    }

    fun cancelAlarm(requestCode: Int, applicationContext: Context) {
        viewModelScope.launch {
            smplrAlarmCancel(applicationContext) {
                requestCode { requestCode }
            }
        }
    }

    fun updateNotification(requestCode: Int, applicationContext: Context) {
        viewModelScope.launch {
            smplrAlarmUpdate(applicationContext) {
                requestCode { requestCode }
                notificationChannel {
                    channel {
                        channelId { "smplr_alarm_notification_channel" }
                        name { "Smplr Alarm Notifications" }
                        description { "Standard SmplrAlarm notifications" }
                        importance { NotificationManager.IMPORTANCE_HIGH }
                        showBadge { true }
                    }
                }
                notification {
                    NotificationItem(
                        R.drawable.ic_baseline_change_circle_24,
                        "I am changed",
                        "I am changed",
                        "I am changed"
                    )
                }
            }
        }
    }

    companion object{
        internal const val ACTION_SNOOZE = "action_snooze"
        internal const val ACTION_DISMISS = "action_dismiss"
        internal const val ACTION_NOTIFICATION_DISMISS = "action_notification_dismiss"
        internal const val HOUR = "hour"
        internal const val MINUTE = "minute"
    }
}