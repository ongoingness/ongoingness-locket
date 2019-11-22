package uk.ac.ncl.openlab.ongoingness.database

import android.content.Context
import androidx.room.*
import uk.ac.ncl.openlab.ongoingness.database.daos.LogDao
import uk.ac.ncl.openlab.ongoingness.database.daos.MediaDateDao
import uk.ac.ncl.openlab.ongoingness.database.daos.WatchMediaDao
import uk.ac.ncl.openlab.ongoingness.database.schemas.Log
import uk.ac.ncl.openlab.ongoingness.database.schemas.MediaDate
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import uk.ac.ncl.openlab.ongoingness.utilities.Converters

@Database(entities = [WatchMedia::class, Log::class, MediaDate::class], version = 8)
@TypeConverters(Converters::class)
abstract class WatchMediaRoomDatabase : RoomDatabase() {

    abstract fun watchMediaDao(): WatchMediaDao
    abstract fun logDao(): LogDao
    abstract fun mediaDateDao(): MediaDateDao

    companion object  {
        @Volatile
        private  var INSTANCE: WatchMediaRoomDatabase? = null

        fun getDatabase(context: Context): WatchMediaRoomDatabase {
            return INSTANCE
                    ?: synchronized(this) {
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

