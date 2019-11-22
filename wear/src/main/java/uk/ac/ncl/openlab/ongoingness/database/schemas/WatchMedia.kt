package uk.ac.ncl.openlab.ongoingness.database.schemas

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.sql.Date

@Entity(tableName = "watch_media")
data class WatchMedia(
        @PrimaryKey @ColumnInfo(name = "_id") val _id: String,
        @ColumnInfo(name = "path")            val path: String,
        @ColumnInfo(name = "collection")      val collection: String,
        @ColumnInfo(name = "mimetype")        val mimetype: String,
        @ColumnInfo(name = "order")           val order: Int,
        @ColumnInfo(name = "createdAt")       val createdAt: Date,
        @ColumnInfo(name = "duration")        var duration: Int)
{

    override fun equals(other: Any?): Boolean {
        if(other == null || other !is WatchMedia)
            return false

        return (this._id == other._id &&
                this.path == other.path &&
                this.collection == other.collection &&
                this.mimetype == other.mimetype &&
                this.createdAt == other.createdAt)
    }
}



