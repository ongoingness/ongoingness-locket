package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WatchMedia::class, Log::class], version = 5)
abstract class WatchMediaRoomDatabase : RoomDatabase() {

    abstract fun watchMediaDao(): WatchMediaDao
    abstract fun logDao(): LogDao

    companion object  {
        @Volatile
        private  var INSTANCE: WatchMediaRoomDatabase? = null

        fun getDatabase(context: Context): WatchMediaRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        WatchMediaRoomDatabase::class.java,
                        "WatchMedia_database").fallbackToDestructiveMigration().allowMainThreadQueries().build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

