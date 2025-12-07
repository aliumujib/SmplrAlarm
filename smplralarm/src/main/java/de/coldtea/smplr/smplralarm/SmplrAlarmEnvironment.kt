package de.coldtea.smplr.smplralarm

import android.content.Context
import de.coldtea.smplr.smplralarm.models.AlarmIdGenerator
import de.coldtea.smplr.smplralarm.models.AlarmScheduler
import de.coldtea.smplr.smplralarm.models.AlarmStore
import de.coldtea.smplr.smplralarm.models.DefaultAlarmIdGenerator
import de.coldtea.smplr.smplralarm.models.DefaultSmplrAlarmLogger
import de.coldtea.smplr.smplralarm.models.SmplrAlarmLogger
import de.coldtea.smplr.smplralarm.repository.RoomAlarmStore
import de.coldtea.smplr.smplralarm.services.AlarmSchedulerImpl
import de.coldtea.smplr.smplralarm.services.AlarmService

/**
 * Global configuration entry point for SmplrAlarm.
 *
 * Apps can call [SmplrAlarmEnvironment.init] once (typically at startup)
 * to supply their own AlarmStore / AlarmScheduler / logger / id generator.
 * All APIs and receivers that use [SmplrAlarmEnvironment.current] will
 * then share the same wiring.
 */
data class SmplrAlarmConfig(
    val storeFactory: (Context) -> AlarmStore,
    val idGenerator: AlarmIdGenerator = DefaultAlarmIdGenerator,
    val logger: SmplrAlarmLogger = DefaultSmplrAlarmLogger,
    val schedulerFactory: (Context) -> AlarmScheduler =
        { context -> AlarmSchedulerImpl(AlarmService(context), storeFactory.invoke(context)) },
)

object SmplrAlarmEnvironment {

    @Volatile
    private var config: SmplrAlarmConfig? = null

    fun init(config: SmplrAlarmConfig) {
        this.config = config
    }

    fun current(context: Context): SmplrAlarmConfig {
        return config ?: SmplrAlarmConfig(
            storeFactory = { RoomAlarmStore(it.applicationContext) },
        )
    }
}
