package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.sql.Date

@Dao
interface WatchMediaDao {

    @Query("SELECT * from watch_media ORDER BY `order` ASC")
    fun getAllMedia(): LiveData<List<WatchMedia>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: WatchMedia)

    @Query("DELETE FROM watch_media")
    fun deleteAll()

    @Query("SELECT * FROM watch_media WHERE _id LIKE :id")
    fun getMedia(id: String) : WatchMedia

    @Query("DELETE FROM watch_media WHERE _id LIKE :id")
    suspend fun delete(id: String)

    @Query("SELECT * from watch_media")
    fun getAll(): List<WatchMedia>


    @Query("SELECT * FROM watch_media JOIN media_date ON media_date.mediaId = watch_media._id WHERE strftime('%j-%Y',date) IS strftime('%j-%Y',:date)")
    fun getForDate(date: Date):List<WatchMedia>

    @Query("SELECT * FROM watch_media JOIN media_date ON media_date.mediaId = watch_media._id WHERE strftime('%j',date) IS strftime('%j',:date)")
    fun getForDayOfYear(date: Date):List<WatchMedia>
}