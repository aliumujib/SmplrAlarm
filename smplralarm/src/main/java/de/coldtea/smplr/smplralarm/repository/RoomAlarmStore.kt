package de.coldtea.smplr.smplralarm.repository

import android.content.Context
import de.coldtea.smplr.smplralarm.models.AlarmDefinition
import de.coldtea.smplr.smplralarm.models.AlarmStore
import de.coldtea.smplr.smplralarm.models.ObservableAlarmStore
import de.coldtea.smplr.smplralarm.models.NotificationChannelItem
import de.coldtea.smplr.smplralarm.models.NotificationItem
import de.coldtea.smplr.smplralarm.models.NotificationConfig
import de.coldtea.smplr.smplralarm.models.NotificationTargetDescriptor
import de.coldtea.smplr.smplralarm.models.WeekDays
import de.coldtea.smplr.smplralarm.repository.store.AlarmDefinitionEntity
import de.coldtea.smplr.smplralarm.repository.store.AlarmStoreDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Opinionated AlarmStore implementation backed by a dedicated Room database
 * (AlarmStoreDatabase). This does not use SharedPreferences or the legacy
 * AlarmNotificationRepository; it is a new storage path for the refactored
 * API.
 */
class RoomAlarmStore(
    context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : ObservableAlarmStore {

    private val db: AlarmStoreDatabase = AlarmStoreDatabase.getInstance(context)
    private val dao = db.daoAlarmDefinition

    override val alarmsFlow: Flow<List<AlarmDefinition>> =
        dao.observeAll().map { list -> list.map { it.toDefinition(json) } }

    override suspend fun insert(definition: AlarmDefinition) {
        val entity = definition.toEntity(json)
        dao.insert(entity)
    }

    override suspend fun update(definition: AlarmDefinition) {
        dao.update(definition.toEntity(json))
    }

    override suspend fun delete(id: Int) {
        val entity = dao.getById(id) ?: return
        dao.delete(entity)
    }

    override suspend fun get(id: Int): AlarmDefinition? {
        return dao.getById(id)?.toDefinition(json)
    }

    override suspend fun getAll(): List<AlarmDefinition> {
        return dao.getAll().map { it.toDefinition(json) }
    }
}

private fun AlarmDefinition.toEntity(json: Json): AlarmDefinitionEntity {
    val weekdaysJson = json.encodeToString(weekdays.map { it.name })
    val metadataJson = json.encodeToString(metadata)

    val channelJson = notificationConfig?.channel?.let { json.encodeToString(it) }
    val notificationJson = notificationConfig?.notification?.let { json.encodeToString(it) }

    val targetsPayload = notificationConfig?.let {
        NotificationTargetsPayload(
            contentTarget = it.contentTarget,
            fullScreenTarget = it.fullScreenTarget,
            alarmReceivedTarget = it.alarmReceivedTarget,
        )
    }
    val targetsJson = targetsPayload?.let { json.encodeToString(it) }

    return AlarmDefinitionEntity(
        id = id,
        hour = hour,
        minute = minute,
        second = second,
        millis = millis,
        weekdaysJson = weekdaysJson,
        isActive = isActive,
        nextTriggerTime = nextTriggerTime,
        metadataJson = metadataJson,
        notificationChannelJson = channelJson,
        notificationJson = notificationJson,
        notificationTargetsJson = targetsJson,
    )
}

private fun AlarmDefinitionEntity.toDefinition(json: Json): AlarmDefinition {
    val weekdayNames: List<String> = json.decodeFromString(weekdaysJson)
    val weekdays = weekdayNames.mapNotNull { name ->
        runCatching { WeekDays.valueOf(name) }.getOrNull()
    }

    val metadata: Map<String, String> = json.decodeFromString(metadataJson)

    val channel: NotificationChannelItem? = notificationChannelJson?.let {
        json.decodeFromString(it)
    }
    val notification: NotificationItem? = notificationJson?.let {
        json.decodeFromString(it)
    }

    val targets: NotificationTargetsPayload? = notificationTargetsJson?.let {
        json.decodeFromString<NotificationTargetsPayload>(it)
    }

    return AlarmDefinition(
        id = id,
        hour = hour,
        minute = minute,
        second = second,
        millis = millis,
        weekdays = weekdays,
        isActive = isActive,
        nextTriggerTime = nextTriggerTime,
        metadata = metadata,
        notificationConfig = if (channel != null || notification != null || targets != null) {
            NotificationConfig(
                channel = channel,
                notification = notification,
                contentTarget = targets?.contentTarget,
                fullScreenTarget = targets?.fullScreenTarget,
                alarmReceivedTarget = targets?.alarmReceivedTarget,
            )
        } else {
            null
        },
    )
}

@kotlinx.serialization.Serializable
private data class NotificationTargetsPayload(
    val contentTarget: NotificationTargetDescriptor? = null,
    val fullScreenTarget: NotificationTargetDescriptor? = null,
    val alarmReceivedTarget: NotificationTargetDescriptor? = null,
)

