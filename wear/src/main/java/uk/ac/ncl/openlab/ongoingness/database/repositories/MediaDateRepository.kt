package uk.ac.ncl.openlab.ongoingness.database.repositories

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import uk.ac.ncl.openlab.ongoingness.database.daos.MediaDateDao
import uk.ac.ncl.openlab.ongoingness.database.schemas.MediaDate

class MediaDateRepository(private val mediaDateDao: MediaDateDao) {

    @WorkerThread
    suspend fun insert(mediaDate: MediaDate){
        mediaDateDao.insert(mediaDate)
    }

    @WorkerThread
    fun get(id:String): MediaDate?{
        return mediaDateDao.getMediaDate(id)
    }


    @WorkerThread
    suspend fun delete(id:String){
        mediaDateDao.delete(id)
    }


    @WorkerThread
    fun deleteAll(){
        mediaDateDao.deleteAll()
    }

    @WorkerThread
    fun getAll():LiveData<List<MediaDate>>{
        return mediaDateDao.getAll()
    }

}