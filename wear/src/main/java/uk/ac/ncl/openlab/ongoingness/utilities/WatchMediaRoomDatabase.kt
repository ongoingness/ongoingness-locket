package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import androidx.room.*

@Database(entities = [WatchMedia::class, Log::class, MediaDate::class], version = 6)
@TypeConverters(Converters::class)
abstract class WatchMediaRoomDatabase : RoomDatabase() {

    abstract fun watchMediaDao(): WatchMediaDao
    abstract fun logDao(): LogDao
    abstract fun mediaDateDao(): MediaDateDao

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

