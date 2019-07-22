package uk.ac.ncl.openlab.ongoingness.utilities

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WatchMedia::class], version = 2)
public abstract class WatchMediaRoomDatabase : RoomDatabase() {

    abstract fun watchMediaDao(): WatchMediaDao

    companion object  {
        @Volatile
        private  var INSTANCE: WatchMediaRoomDatabase? = null

        fun getDatabase(context: Context): WatchMediaRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        WatchMediaRoomDatabase::class.java,
                        "WatchMedia_database").fallbackToDestructiveMigration().build();
                INSTANCE = instance
                return instance
            }
        }
    }
}

