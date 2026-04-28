package com.semseytech.rtsdevicesuitepro.organizer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [OrganizerRuleEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class OrganizerDatabase : RoomDatabase() {
    abstract fun organizerDao(): OrganizerDao

    companion object {
        @Volatile
        private var INSTANCE: OrganizerDatabase? = null

        fun getDatabase(context: Context): OrganizerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OrganizerDatabase::class.java,
                    "organizer_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
