package de.coldtea.smplr.smplralarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.coldtea.smplr.smplralarm.models.SmplrAlarmReceiverObjects
import de.coldtea.smplr.smplralarm.models.toIntent
import de.coldtea.smplr.smplralarm.repository.RoomAlarmStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal class ActivateAppReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val requestId =
            intent.getIntExtra(SmplrAlarmReceiverObjects.SMPLR_ALARM_RECEIVER_INTENT_ID, -1)

        onAlarmIndicatorTapped(context, requestId)
    }

    private fun onAlarmIndicatorTapped(context: Context, requestId: Int) = CoroutineScope(Dispatchers.IO).launch {
        if (requestId == -1) return@launch

        val store = RoomAlarmStore(context)
        val definition = store.get(requestId) ?: return@launch
        val target = definition.notificationConfig?.contentTarget ?: return@launch

        val intent = target.toIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(intent)
    }

}