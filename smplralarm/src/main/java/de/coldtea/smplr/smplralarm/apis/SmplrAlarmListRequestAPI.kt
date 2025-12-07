package de.coldtea.smplr.smplralarm.apis

import android.content.Context
import de.coldtea.smplr.smplralarm.models.ActiveAlarmList
import de.coldtea.smplr.smplralarm.models.AlarmDefinition
import de.coldtea.smplr.smplralarm.models.AlarmItem
import de.coldtea.smplr.smplralarm.models.WeekDays
import de.coldtea.smplr.smplralarm.models.AlarmStore
import de.coldtea.smplr.smplralarm.repository.RoomAlarmStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by [Yasar Naci Gündüz](https://github.com/ColdTea-Projects).
 *
 * In the refactored design this uses AlarmStore (RoomAlarmStore by default)
 * as the source of truth and adapts it to the legacy JSON shape used by
 * existing consumers.
 */
class SmplrAlarmListRequestAPI(
    val context: Context,
    private val store: AlarmStore = RoomAlarmStore(context),
) {

    private val json = Json { ignoreUnknownKeys = true }

    internal var alarmListChangeOrRequestedListener: ((String) -> Unit)? = null

    fun requestAlarmList() {
        CoroutineScope(Dispatchers.IO).launch {
            val definitions: List<AlarmDefinition> = store.getAll()

            val activeAlarmList = ActiveAlarmList(
                definitions.map { def ->
                    AlarmItem(
                        requestId = def.id,
                        hour = def.hour,
                        minute = def.minute,
                        weekDays = def.weekdays,
                        isActive = def.isActive,
                        infoPairs = json.encodeToString(def.metadata),
                    )
                }
            )

            alarmListChangeOrRequestedListener?.invoke(json.encodeToString(activeAlarmList))
        }
    }
}