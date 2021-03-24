package uk.ac.ncl.openlab.ongoingness.database.repositories

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import uk.ac.ncl.openlab.ongoingness.database.daos.MediaDateDao
import uk.ac.ncl.openlab.ongoingness.database.schemas.MediaDate

/**
 * Repository for MediaDates.
 *
 * @param mediaDateDao Data access object to MediaDate.
 *
 * @author Luis Carvalho
 */
class MediaDateRepository(private val mediaDateDao: MediaDateDao) {

    /**
     * Adds a new MediaDate to the database.
     *
     * @param mediaDate new MediaFate to be added.
     */
    @WorkerThread
    suspend fun insert(mediaDate: MediaDate){
        mediaDateDao.insert(mediaDate)
    }

    /**
     * Get a MediaDate given an id.
     *
     * @param id id of the MediaDate.
     * @return the MediaDate with the given id or null.
     */
    @WorkerThread
    fun get(id:String): MediaDate?{
        return mediaDateDao.getMediaDate(id)
    }

    /**
     * Deletes a MediaDate given an id.
     *
     * @param id id of the Media Date to be deleted.
     */
    @WorkerThread
    suspend fun delete(id:String){
        mediaDateDao.delete(id)
    }

    /**
     * Deletes all MediaDates in the database.
     */
    @WorkerThread
    fun deleteAll(){
        mediaDateDao.deleteAll()
    }

    /**
     * Gets all MediaDates all as LiveData list.
     *
     * @return LiveData list with all MediaDate.
     */
    @WorkerThread
    fun getAll():LiveData<List<MediaDate>>{
        return mediaDateDao.getAll()
    }

}