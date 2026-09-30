/* NES emulator By ArDev */
package com.example.nes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room Database for NES Emulator local storage.
 * Stores Save States metadata and configuration.
 * NES emulator By ArDev
 */
@Database(
    entities = [SaveStateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NesDatabase : RoomDatabase() {

    abstract fun saveStateDao(): SaveStateDao

    companion object {
        @Volatile
        private var INSTANCE: NesDatabase? = null

        fun getDatabase(context: Context): NesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NesDatabase::class.java,
                    "nes_emulator_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
