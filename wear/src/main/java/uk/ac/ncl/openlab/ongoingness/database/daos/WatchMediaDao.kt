package uk.ac.ncl.openlab.ongoingness.database.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia

/**
 * Data access object for WatchMedia.
 *
 * @author Luis Carvalho
 */
@Dao
interface WatchMediaDao {

    /**
     * Gets all WatchMedia all as LiveData list.
     *
     * @return LiveData list with all WatchMedia.
     */
    @Query("SELECT * from watch_media ORDER BY `order` ASC")
    fun getAllMedia(): LiveData<List<WatchMedia>>

    /**
     * Insert a new WatchMedia into the database.
     *
     * @param media WatchMedia to be added.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(media: WatchMedia)

    /**
     * Deletes all WatchMedia from the database.
     */
    @Query("DELETE FROM watch_media")
    fun deleteAll()

    /**
     * Get a WatchMedia given its id.
     *
     * @param id id of the WatchMedia.
     * @return WatchMedia with the given id.
     */
    @Query("SELECT * FROM watch_media WHERE _id LIKE :id")
    fun getMedia(id: String) : WatchMedia

    /**
     * Delete a WatchMedia given its id.
     *
     * @param id id of the WatchMedia to be deleted.
     */
    @Query("DELETE FROM watch_media WHERE _id LIKE :id")
    suspend fun delete(id: String)

    /**
     * Get all WatchMedia from the database.
     *
     * @return list with all WatchMedia.
     */
    @Query("SELECT * from watch_media")
    fun getAll(): List<WatchMedia>

    /**
     * Get a WatchMedia belonging to a collection.
     *
     * @param collection name of the collection.
     * @return list of WatchMedia belonging to the collection.
     */
    @Query("SELECT * FROM watch_media WHERE collection LIKE :collection")
    fun getCollection(collection: String): List<WatchMedia>

    /**
     * Get WatchMedia associated to a date.
     *
     * @param day day of the month.
     * @param month number of the month in a year.
     * @return list of WatchMedia associated to the date.
     */
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM watch_media JOIN media_date ON media_date.mediaId = watch_media._id WHERE media_date.day IS :day AND media_date.month IS :month")
    fun getMediaForDayOfMonth(day: Int, month: Int):List<WatchMedia>

    /**
     * Get WatchMedia with no dates associated.
     *
     * @return list of all WatchMedia with no WatchMedia.
     */
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM watch_media LEFT OUTER JOIN media_date ON media_date.mediaId = watch_media._id")
    fun getMediaWithNoDates(): List<WatchMedia>

}