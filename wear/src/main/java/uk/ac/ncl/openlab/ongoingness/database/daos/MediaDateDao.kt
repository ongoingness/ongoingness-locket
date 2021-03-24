package uk.ac.ncl.openlab.ongoingness.database.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.ac.ncl.openlab.ongoingness.database.schemas.MediaDate

/**
 * Data access object for MediaDates.
 *
 * @author Luis Carvalho
 */
@Dao
interface MediaDateDao {

    /**
     * Gets all MediaDates all as LiveData list.
     *
     * @return LiveData list with all MediaDate.
     */
    @Query("SELECT * FROM media_date ORDER BY date ASC")
    fun getAll(): LiveData<List<MediaDate>>

    /**
     * Adds a new MediaDate to the database.
     *
     * @param mediaDate new MediaFate to be added.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mediaDate: MediaDate)

    /**
     * Deletes all MediaDates in the database.
     */
    @Query("DELETE FROM media_date")
    fun deleteAll()

    /**
     * Deletes a MediaDate given the id of the associated WatchMedia.
     *
     * @param mediaId id of the associated WatchMedia.
     */
    @Query("DELETE FROM media_date WHERE mediaId LIKE :mediaId")
    fun deleteMedia(mediaId: String)

    /**
     * Get a MediaDate given an id.
     *
     * @param id id of the MediaDate.
     * @return the MediaDate with the given id or null.
     */
    @Query("SELECT * FROM media_date WHERE _id LIKE :id")
    fun getMediaDate(id: String) : MediaDate?

    /**
     * Get a list of MediaDates associated to a WatchMedia.
     *
     * @param mediaId id of the WatchMedia.
     * @return LiveData list of MediaDates associated to a WatchMedia.
     */
    @Query("SELECT * FROM media_date WHERE mediaId LIKE :mediaId")
    fun getMedia(mediaId: String) : List<MediaDate>?

    /**
     * Deletes a MediaDate given an id.
     *
     * @param id id of the Media Date to be deleted.
     */
    @Query("DELETE FROM media_date WHERE _id LIKE :id")
    suspend fun delete(id: String)

    /**
     * Gets a list of MediaDates associated to a date.
     *
     * @param day day of the month.
     * @param month number of the month in a year.
     * @return list of MediaDates associated.
     */
    @Query("SELECT * from media_date WHERE media_date.day IS :day AND media_date.month IS :month")
    fun getDayOfTheMonth(day: Int, month: Int) : List<MediaDate>?

}