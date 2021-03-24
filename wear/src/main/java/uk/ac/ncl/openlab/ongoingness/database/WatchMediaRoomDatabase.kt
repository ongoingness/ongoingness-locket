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


/**
 * Database containing WatchMedia, Logs, and MediaDate.
 *
 * @author Luis Carvalho
 */
@Database(entities = [WatchMedia::class, Log::class, MediaDate::class], version = 8)
@TypeConverters(Converters::class)
abstract class WatchMediaRoomDatabase : RoomDatabase() {

    /**
     * Data access object for WatchMedia.
     */
    abstract fun watchMediaDao(): WatchMediaDao

    /**
     * Data access object for Logs.
     */
    abstract fun logDao(): LogDao

    /**
     * Data access object for MediaDates.
     */
    abstract fun mediaDateDao(): MediaDateDao

    /**
     * Singleton providing access to the database.
     */
    companion object  {

        /**
         * Instance of the local database.
         */
        @Volatile
        private  var INSTANCE: WatchMediaRoomDatabase? = null

        /**
         * Gets the local database.
         *
         * @param context context of the application.
         * @return the local database access.
         */
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

