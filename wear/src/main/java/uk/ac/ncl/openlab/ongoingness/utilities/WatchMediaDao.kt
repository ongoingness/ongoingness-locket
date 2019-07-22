package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WatchMediaDao {

    @Query("SELECT * from watch_media ORDER BY `order` ASC")
    fun getAllMedia(): LiveData<List<WatchMedia>>;

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: WatchMedia)

    @Query("DELETE FROM watch_media")
    fun deleteAll()

    @Query("SELECT * FROM watch_media WHERE _id LIKE :id")
    fun getMedia(id: String) : WatchMedia

    @Query("DELETE FROM watch_media WHERE _id LIKE :id")
    suspend  fun delete(id: String)

    @Query("SELECT * from watch_media")
    fun getAll(): List<WatchMedia>
}