package de.coldtea.smplr.smplralarm.models

import androidx.annotation.DrawableRes
import kotlinx.serialization.Serializable

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */
@Serializable
data class NotificationChannelItem(
    val channelId: String,
    val importance: Int,
    val showBadge: Boolean,
    val name: String,
    val description: String
)

@Serializable
data class NotificationItem(
    val smallIcon: Int? = null,
    val title: String? = null,
    val message: String? = null,
    val bigText: String? = null,
    val autoCancel: Boolean? = null,
    val firstButtonText: String? = null,
    val secondButtonText: String? = null,
    val firstButtonTarget: NotificationTargetDescriptor? = null,
    val secondButtonTarget: NotificationTargetDescriptor? = null,
    val dismissTarget: NotificationTargetDescriptor? = null,
)
