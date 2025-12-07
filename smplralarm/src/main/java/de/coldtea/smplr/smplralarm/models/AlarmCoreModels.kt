package de.coldtea.smplr.smplralarm.models

import kotlinx.serialization.Serializable

/**
 * Core alarm definition used by the new storage/scheduling abstractions.
 *
 * This is intentionally storage-agnostic. Concrete stores (e.g. Room-based
 * implementations) are responsible for mapping this model to their schemas.
 */
@Serializable
data class AlarmDefinition(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val second: Int = 0,
    val millis: Int = 0,
    val weekdays: List<WeekDays>,
    val isActive: Boolean,
    val nextTriggerTime: Long? = null,
    val metadata: Map<String, String>,
    val notificationConfig: NotificationConfig?,
)

/**
 * Notification configuration associated with an alarm.
 *
 * This is a thin wrapper around existing notification/channel models so that
 * higher layers can reason about scheduling separately from UI concerns. It
 * also carries stable descriptors for activation targets instead of raw
 * Intents.
 */
@Serializable
data class NotificationConfig(
    val channel: NotificationChannelItem?,
    val notification: NotificationItem?,
    val contentTarget: NotificationTargetDescriptor? = null,
    val fullScreenTarget: NotificationTargetDescriptor? = null,
    val alarmReceivedTarget: NotificationTargetDescriptor? = null,
)

/**
 * Serializable descriptor for a notification activation target. This is
 * inspired by the FocusModes NotificationActionTarget model, but stores
 * class names and extras as simple strings so it can be serialized via
 * kotlinx.serialization and persisted in Room.
 */
@Serializable
sealed class NotificationTargetDescriptor {

    abstract val action: String?
    abstract val extras: Map<String, String>

    @Serializable
    data class ScreenTarget(
        val packageName: String,
        val activityClassName: String,
        override val action: String? = null,
        override val extras: Map<String, String> = emptyMap(),
    ) : NotificationTargetDescriptor()

    @Serializable
    data class ServiceTarget(
        val packageName: String,
        val serviceClassName: String,
        override val action: String? = null,
        override val extras: Map<String, String> = emptyMap(),
    ) : NotificationTargetDescriptor()

    @Serializable
    data class BroadcastTarget(
        val packageName: String,
        val receiverClassName: String,
        override val action: String? = null,
        override val extras: Map<String, String> = emptyMap(),
    ) : NotificationTargetDescriptor()
}

/**
 * Abstract storage for alarms. Default implementation will be backed by
 * Room + SharedPreferences, but advanced consumers can provide their own
 * implementation.
 */
interface AlarmStore {
    suspend fun insert(definition: AlarmDefinition)
    suspend fun update(definition: AlarmDefinition)
    suspend fun delete(id: Int)
    suspend fun get(id: Int): AlarmDefinition?
    suspend fun getAll(): List<AlarmDefinition>
}

/**
 * Optional reactive extension of AlarmStore. Implementations expose a Flow
 * of all definitions so callers can observe changes over time.
 */
interface ObservableAlarmStore : AlarmStore {
    val alarmsFlow: kotlinx.coroutines.flow.Flow<List<AlarmDefinition>>
}

/**
 * Helper to safely cast an AlarmStore to ObservableAlarmStore when supported.
 */
fun AlarmStore.asObservable(): ObservableAlarmStore? = this as? ObservableAlarmStore

/**
 * Abstraction over the actual scheduling mechanism (AlarmManager, etc.).
 */
interface AlarmScheduler {
    fun schedule(
        id: Int,
        hour: Int,
        minute: Int,
        second: Int = 0,
        weekDays: List<WeekDays>,
    )

    fun rescheduleTomorrow(definition: AlarmDefinition)

    fun renew(definition: AlarmDefinition)

    fun cancel(id: Int)

    fun exists(id: Int): Boolean
}

/**
 * Abstraction for generating alarm IDs, so callers that require stable or
 * externally-defined request codes can plug in their own strategy.
 */
fun interface AlarmIdGenerator {
    fun generateId(): Int
}

object DefaultAlarmIdGenerator : AlarmIdGenerator {
    override fun generateId(): Int = System.currentTimeMillis().toInt()
}
