package de.coldtea.smplr.smplralarm.repository.store

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface DaoAlarmDefinition {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AlarmDefinitionEntity): Long

    @Update
    suspend fun update(entity: AlarmDefinitionEntity)

    @Delete
    suspend fun delete(entity: AlarmDefinitionEntity)

    @Query("SELECT * FROM alarm_definition WHERE id = :id")
    suspend fun getById(id: Int): AlarmDefinitionEntity?

    @Query("SELECT * FROM alarm_definition")
    suspend fun getAll(): List<AlarmDefinitionEntity>

    @Query("SELECT * FROM alarm_definition")
    fun observeAll(): Flow<List<AlarmDefinitionEntity>>
}
