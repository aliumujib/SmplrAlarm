package de.coldtea.smplr.smplralarm.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.coldtea.smplr.smplralarm.SmplrAlarmEnvironment
import de.coldtea.smplr.smplralarm.models.launchIo

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 */

internal class TimeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val config = SmplrAlarmEnvironment.current(context)
        val logger = config.logger
        logger.i("onRecieve --> ${intent.action}")
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> onBootComplete(context)
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
                definitions.filter { it.isActive }.forEach { definition ->
                    scheduler.renew(definition)
                }
            }.onFailure { throwable ->
                logger.e("TimeChangeReceiver reschedule failed: ${throwable.message}", throwable)
            }
        }
    }

    companion object {
        fun build(context: Context): Intent {
            return Intent(context, TimeChangeReceiver::class.java)
        }
    }

}