package de.coldtea.smplr.smplralarm.repository.store

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AlarmDefinitionEntity::class],
    version = 2,
    exportSchema = false,
)
internal abstract class AlarmStoreDatabase : RoomDatabase() {

    abstract val daoAlarmDefinition: DaoAlarmDefinition

    companion object {
        @Volatile
        private var INSTANCE: AlarmStoreDatabase? = null

        fun getInstance(context: Context): AlarmStoreDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlarmStoreDatabase::class.java,
                    "db_smplr_alarm_store",
                ).addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE alarm_definition ADD COLUMN notificationTargetsJson TEXT"
                )
            }
        }
    }
}
