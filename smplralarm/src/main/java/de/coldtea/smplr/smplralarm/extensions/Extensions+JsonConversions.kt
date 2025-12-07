package de.coldtea.smplr.smplralarm.extensions

import de.coldtea.smplr.smplralarm.models.ActiveAlarmList
import de.coldtea.smplr.smplralarm.models.ActiveWeekDays
import de.coldtea.smplr.smplralarm.models.WeekDays
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal fun List<WeekDays>.activeDaysAsJsonString(): String =
    Json.encodeToString(ActiveWeekDays(this))

internal fun List<Pair<String, String>>?.convertToJson(): String =
    "{${this?.joinToString(separator = ",") { "\"${it.first}\" : \"${it.second}\"" }.orEmpty()}}"
