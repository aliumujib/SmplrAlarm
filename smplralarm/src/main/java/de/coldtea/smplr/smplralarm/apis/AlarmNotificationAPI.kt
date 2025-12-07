package de.coldtea.smplr.smplralarm.apis

import androidx.annotation.DrawableRes
import de.coldtea.smplr.smplralarm.models.NotificationItem
import de.coldtea.smplr.smplralarm.models.NotificationTargetDescriptor

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */
class AlarmNotificationAPI {

    //region properties

    @DrawableRes
    internal var smallIcon: Int? = null
    internal var title: String? = null
    internal var message: String? = null
    internal var bigText: String? = null
    internal var autoCancel: Boolean? = null
    internal var firstButtonText: String? = null
    internal var secondButtonText: String? = null
    internal var firstButtonTarget: NotificationTargetDescriptor? = null
    internal var secondButtonTarget: NotificationTargetDescriptor? = null
    internal var dismissTarget: NotificationTargetDescriptor? = null

    //endregion

    //region setters

    fun smallIcon(smallIcon: () -> Int) {
        this.smallIcon = smallIcon()
    }

    fun title(title: () -> String) {
        this.title = title()
    }

    fun message(message: () -> String) {
        this.message = message()
    }

    fun bigText(bigText: () -> String) {
        this.bigText = bigText()
    }

    fun autoCancel(autoCancel: () -> Boolean) {
        this.autoCancel = autoCancel()
    }

    fun firstButtonText(firstButtonText: () -> String) {
        this.firstButtonText = firstButtonText()
    }

    fun secondButtonText(secondButtonText: () -> String) {
        this.secondButtonText = secondButtonText()
    }

    fun firstButtonTarget(firstButtonTarget: () -> NotificationTargetDescriptor) {
        this.firstButtonTarget = firstButtonTarget()
    }

    fun secondButtonTarget(secondButtonTarget: () -> NotificationTargetDescriptor) {
        this.secondButtonTarget = secondButtonTarget()
    }

    fun dismissTarget(dismissTarget: () -> NotificationTargetDescriptor) {
        this.dismissTarget = dismissTarget()
    }

    //endregion

    //region build

    internal fun build(): NotificationItem =
        NotificationItem(
            smallIcon = smallIcon,
            title = title,
            message = message,
            bigText = bigText,
            autoCancel = autoCancel,
            firstButtonText = firstButtonText,
            secondButtonText = secondButtonText,
            firstButtonTarget = firstButtonTarget,
            secondButtonTarget = secondButtonTarget,
            dismissTarget = dismissTarget,
        )

    //endregion
}