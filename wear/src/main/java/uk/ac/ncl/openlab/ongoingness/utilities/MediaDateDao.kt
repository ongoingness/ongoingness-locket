package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.sql.Date


@Dao
interface MediaDateDao {

    @Query("SELECT * FROM media_date ORDER BY date ASC")
    fun getAll(): LiveData<List<MediaDate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mediaDate: MediaDate)

    @Query("DELETE FROM media_date")
    fun deleteAll()

    @Query("DELETE FROM media_date WHERE mediaId LIKE :mediaId")
    fun deleteMedia(mediaId: String)

    @Query("SELECT * FROM media_date WHERE _id LIKE :id")
    fun getMediaDate(id: String) : MediaDate?

    @Query("SELECT * FROM media_date WHERE mediaId LIKE :mediaId")
    fun getMedia(mediaId: String) : List<MediaDate>?

    @Query("DELETE FROM watch_media WHERE _id LIKE :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM media_date WHERE strftime('%j',date) IS strftime('%j',:date)")
    fun getDayOfYear(date: Date):List<MediaDate>

    @Query("SELECT * FROM media_date WHERE strftime('%j-%Y',date) IS strftime('%j-%Y',:date)")
    fun getDate(date: Date):List<MediaDate>


}