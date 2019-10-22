package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import java.sql.Date

@Entity(tableName = "watch_media")
data class WatchMedia(
        @PrimaryKey @ColumnInfo(name = "_id") val _id: String,
        @ColumnInfo(name = "path")            val path: String,
        @ColumnInfo(name = "collection")      val collection: String,
        @ColumnInfo(name = "mimetype")        val mimetype: String,
        @ColumnInfo(name = "datetime")        val datetime: Date?,
        @ColumnInfo(name = "order")           val order: Int) {


    override fun equals(other: Any?): Boolean {
        if(other == null || other !is WatchMedia)
            return false

        return (this._id == other._id &&
                this.path == other.path &&
                this.collection == other.collection &&
                this.mimetype == other.mimetype)
    }

    companion object {
        fun longToDate(long:Long?):Date?{
            return if(long != null){
                Date(long)
            }else{
                null
            }
        }

        fun longsToDate(longs:JSONArray?):Date?{
            return if(longs != null && longs.length() > 0){
                Date(longs[0] as Long)
            }else{
                null
            }
        }
    }
}

