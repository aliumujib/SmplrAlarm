package de.coldtea.smplr.smplralarm.models

import kotlinx.serialization.Serializable

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */
@Serializable
internal data class ActiveWeekDays(
    val days: List<WeekDays>
)