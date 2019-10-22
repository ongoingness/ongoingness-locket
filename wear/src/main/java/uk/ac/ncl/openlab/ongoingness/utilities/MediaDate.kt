package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.room.*
import org.json.JSONArray
import java.sql.Date

@Entity(tableName = "media_date",
        indices = [Index(value = ["mediaId"], unique = false)],
        foreignKeys = [ForeignKey(entity = WatchMedia::class,
        parentColumns = ["_id"],
        childColumns = ["mediaId"],
        onDelete = ForeignKey.CASCADE)])
data class MediaDate (
    @ColumnInfo(name = "date") val date: Date,
    @ColumnInfo(name = "mediaId") val mediaId:String){

    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id")   var id: Int =  0


    companion object {
        fun longToDate(long:Long):Date{
            return Date(long)
        }
    }
}


