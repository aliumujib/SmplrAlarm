package de.coldtea.smplr.smplralarm.repository.store

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * New, opinionated storage model for AlarmDefinition.
 *
 * This intentionally does not mirror the legacy AlarmNotificationEntity
 * structure. It stores only the scheduling core + serialized metadata/
 * notification information.
 */

@Entity(tableName = "alarm_definition")
internal data class AlarmDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val millis: Int,
    val weekdaysJson: String,
    val isActive: Boolean,
    val nextTriggerTime: Long?,
    val metadataJson: String,
    val notificationChannelJson: String?,
    val notificationJson: String?,
    val notificationTargetsJson: String?,
)
