package uk.ac.ncl.openlab.ongoingness.database.schemas

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log")
data class Log(
        @ColumnInfo(name = "level")                                 val level: String,
        @ColumnInfo(name = "code")                                  val code: String,
        @ColumnInfo(name = "content")                               val content: String,
        @ColumnInfo(name = "message")                               val message: String,
        @ColumnInfo(name = "timestamp")                             val timestamp: String)
{
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id")   var id: Int =  0


    override fun toString(): String {
        return  "{level:$level, code:$code, content:$content, message:$message, timestamp:$timestamp}"
    }

}



