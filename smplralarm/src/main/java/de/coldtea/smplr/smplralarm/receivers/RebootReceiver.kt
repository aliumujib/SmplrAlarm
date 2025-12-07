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

internal class RebootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        SmplrAlarmLoggerHolder.logger.i("onRecieve --> ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> onBootComplete(context)
            else -> SmplrAlarmLoggerHolder.logger.w("onRecieve --> Recieved illegal broadcast!")
        }
    }

    private fun onBootComplete(context: Context) =
        runCatching {
            val store = RoomAlarmStore(context)
            val scheduler = AlarmSchedulerImpl(AlarmService(context), store)

            launchIo {
                runCatching {
                    val definitions = store.getAll()
                    val now = System.currentTimeMillis()

                    definitions
                        .asSequence()
                        .filter { it.isActive }
                        .filter { def ->
                            // Conservative policy: reschedule if we either don't know
                            // nextTriggerTime yet (null) or it is still in the future.
                            val next = def.nextTriggerTime
                            next == null || next >= now
                        }
                        .forEach { definition ->
                            scheduler.schedule(
                                id = definition.id,
                                hour = definition.hour,
                                minute = definition.minute,
                                second = definition.second,
                                weekDays = definition.weekdays,
                            )
                        }
                }.onFailure { throwable ->
                    SmplrAlarmLoggerHolder.logger.e("RebootReceiver scheduling failed: ${throwable.message}", throwable)
                }
            }
        }.onFailure { throwable ->
            SmplrAlarmLoggerHolder.logger.e("onBootComplete failed: ${throwable.message}", throwable)
        }


    companion object {
        fun build(context: Context): Intent {
            return Intent(context, RebootReceiver::class.java)
        }
    }

}