package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_media")
data class WatchMedia(
        @PrimaryKey @ColumnInfo(name = "_id") val _id: String,
        @ColumnInfo(name = "path")            val path: String,
        @ColumnInfo(name = "collection")      val collection: String,
        @ColumnInfo(name = "mimetype")        val mimetype: String,
        @ColumnInfo(name = "order")           val order: Int)
