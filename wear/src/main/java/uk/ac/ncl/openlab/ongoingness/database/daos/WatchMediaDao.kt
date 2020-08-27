package uk.ac.ncl.openlab.ongoingness.database.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
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

    @Query("SELECT * FROM watch_media WHERE collection LIKE :collection")
    fun getCollection(collection: String): List<WatchMedia>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM watch_media JOIN media_date ON media_date.mediaId = watch_media._id WHERE media_date.day IS :day AND media_date.month IS :month")
    fun getMediaForDayOfMonth(day: Int, month: Int):List<WatchMedia>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM watch_media LEFT OUTER JOIN media_date ON media_date.mediaId = watch_media._id")
    fun getMediaWithNoDates(): List<WatchMedia>

}