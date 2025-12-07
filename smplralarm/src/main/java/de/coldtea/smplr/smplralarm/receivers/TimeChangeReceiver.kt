package de.coldtea.smplr.smplralarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.coldtea.smplr.smplralarm.repository.RoomAlarmStore
import de.coldtea.smplr.smplralarm.services.AlarmSchedulerImpl
import de.coldtea.smplr.smplralarm.services.AlarmService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import de.coldtea.smplr.smplralarm.models.launchIo
import de.coldtea.smplr.smplralarm.models.SmplrAlarmLoggerHolder

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        SmplrAlarmLoggerHolder.logger.i("onRecieve --> ${intent.action}")
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> onBootComplete(context)
            else -> SmplrAlarmLoggerHolder.logger.w("onRecieve --> Recieved illegal broadcast!")
        }
    }

    private fun onBootComplete(context: Context) =
        runCatching {
            launchIo {
                runCatching {
                    val store = RoomAlarmStore(context)
                    val scheduler = AlarmSchedulerImpl(AlarmService(context), store)

                    val definitions = store.getAll()
                    definitions.filter { it.isActive }.forEach { definition ->
                        scheduler.renew(definition)
                    }
                }.onFailure { throwable ->
                    SmplrAlarmLoggerHolder.logger.e("TimeChangeReceiver reschedule failed: ${throwable.message}", throwable)
                }
            }
        }.onFailure { throwable ->
            SmplrAlarmLoggerHolder.logger.e("onBootComplete failed: ${throwable.message}", throwable)
        }

    companion object {
        fun build(context: Context): Intent {
            return Intent(context, TimeChangeReceiver::class.java)
        }
    }

}