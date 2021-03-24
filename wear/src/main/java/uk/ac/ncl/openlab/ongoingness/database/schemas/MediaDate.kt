package uk.ac.ncl.openlab.ongoingness.database.schemas

import androidx.room.*
import java.sql.Date

/**
 * Database representation of a date associated to the media.
 *
 * @param id id of the MediaDate.
 * @param date date to be stored.
 * @param mediaId id of the media item associated to the date.
 * @param day day of the month.
 * @param month month of the year as a number.
 *
 * @author Luis Carvalho
 */
@Entity(tableName = "media_date",
        indices = [Index(value = ["mediaId"], unique = false)],
        foreignKeys = [ForeignKey(entity = WatchMedia::class,
        parentColumns = ["_id"],
        childColumns = ["mediaId"],
        onDelete = ForeignKey.CASCADE)])
data class MediaDate (
    @ColumnInfo(name = "date") val date: Date,
    @ColumnInfo(name = "mediaId") val mediaId: String,
    @ColumnInfo(name = "day") val day: Int,
    @ColumnInfo(name = "month") val month: Int)
{

    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id")   var id: Int =  0

    companion object {
        fun longToDate(long:Long):Date{
            return Date(long)
        }
    }
}


