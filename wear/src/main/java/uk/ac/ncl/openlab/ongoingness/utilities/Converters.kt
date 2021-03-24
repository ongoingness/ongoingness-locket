package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.room.TypeConverter
import java.sql.Date

/**
 * Converters used by the Android Rooms library.
 *
 * @author Luis Carvalho
 */
class Converters {

    /**
     * Converts a long timestamp into a SQL date.
     *
     * @param value timestamp to be converted.
     * @return timestamp as a date.
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return if (value == null) null else Date(value)
    }

    /**
     * Converts a SQL date into a long timestamp.
     *
     * @param date date to be converted.
     * @return date as a long timestamp.
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return (date?.time)
    }
}