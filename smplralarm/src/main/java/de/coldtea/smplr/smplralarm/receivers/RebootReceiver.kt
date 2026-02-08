package de.coldtea.smplr.smplralarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.coldtea.smplr.smplralarm.SmplrAlarmEnvironment
import de.coldtea.smplr.smplralarm.models.launchIo

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal class RebootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val config = SmplrAlarmEnvironment.current(context)
        val logger = config.logger

        logger.i("onRecieve --> ${intent.action}")
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> onBootComplete(context)

            else -> logger.w("onRecieve --> Recieved illegal broadcast!")
        }
    }

    private fun onBootComplete(context: Context) {

        val config = SmplrAlarmEnvironment.current(context)
        val store = config.storeFactory(context)
        val scheduler = config.schedulerFactory(context)
        val logger = config.logger

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
                logger.e("RebootReceiver scheduling success")
            }.onFailure { throwable ->
                logger.e("RebootReceiver scheduling failed: ${throwable.message}", throwable)
            }
        }
    }


    companion object {
        fun build(context: Context): Intent {
            return Intent(context, RebootReceiver::class.java)
        }
    }

}