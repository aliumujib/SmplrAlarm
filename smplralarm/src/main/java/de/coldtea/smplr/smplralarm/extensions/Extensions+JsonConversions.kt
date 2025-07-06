package de.coldtea.smplr.smplralarm.extensions

import de.coldtea.smplr.smplralarm.models.ActiveAlarmList
import de.coldtea.smplr.smplralarm.models.ActiveWeekDays
import de.coldtea.smplr.smplralarm.models.WeekDays
import de.coldtea.smplr.smplralarm.repository.entity.AlarmNotificationEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal fun List<WeekDays>.activeDaysAsJsonString(): String =
    Json.encodeToString(ActiveWeekDays(this))

internal fun AlarmNotificationEntity.activeDaysAsWeekdaysList(): List<WeekDays>? =
    try {
        if (this.weekDays.isBlank() || this.weekDays == "[]") {
            listOf()
        } else {
            Json.decodeFromString<ActiveWeekDays>(this.weekDays).days
        }
    } catch (ex: Exception) {
        null
    }

internal fun ActiveAlarmList.alarmsAsJsonString(): String? =
    try {
        Json.encodeToString(this)
    } catch (ex: Exception) {
        null
    }

internal fun List<Pair<String, String>>?.convertToJson(): String =
    "{${this?.joinToString(separator = ",") { "\"${it.first}\" : \"${it.second}\"" }.orEmpty()}}"


