package de.coldtea.smplr.smplralarm.models

import kotlinx.serialization.Serializable

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */
@Serializable
internal data class AlarmItem(
    val requestId: Int,
    val hour: Int,
    val minute: Int,
    val weekDays: List<WeekDays>,
    val isActive: Boolean,
    val infoPairs: String
)