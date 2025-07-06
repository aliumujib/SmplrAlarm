package de.coldtea.smplr.smplralarm.models

import kotlinx.serialization.Serializable

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */
@Serializable
internal data class ActiveAlarmList(
    val alarmItems: List<AlarmItem>
)