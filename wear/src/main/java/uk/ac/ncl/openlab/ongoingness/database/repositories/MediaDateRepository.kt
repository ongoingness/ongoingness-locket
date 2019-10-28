package uk.ac.ncl.openlab.ongoingness.database.repositories

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import uk.ac.ncl.openlab.ongoingness.database.daos.MediaDateDao
import uk.ac.ncl.openlab.ongoingness.database.schemas.MediaDate
import java.sql.Date

class MediaDateRepository(private val mediaDateDao: MediaDateDao) {

    val allMediaDates: LiveData<List<MediaDate>> = mediaDateDao.getAll()

    @WorkerThread
    suspend fun insert(mediaDate: MediaDate){
        mediaDateDao.insert(mediaDate)
    }

    @WorkerThread
    fun get(id:String): MediaDate?{
        return mediaDateDao.getMediaDate(id)
    }

    @WorkerThread
    fun getMedia(mediaId:String):List<MediaDate>?{
        return mediaDateDao.getMedia(mediaId)
    }

    @WorkerThread
    suspend fun delete(id:String){
        mediaDateDao.delete(id)
    }

    @WorkerThread
    suspend fun deleteMedia(mediaId:String){
        mediaDateDao.deleteMedia(mediaId)
    }

    @WorkerThread
    fun deleteAll(){
        mediaDateDao.deleteAll()
    }

    @WorkerThread
    fun getAll():LiveData<List<MediaDate>>{
        return mediaDateDao.getAll()
    }

    @WorkerThread
    fun getToday():List<MediaDate>{
        return mediaDateDao.getDayOfYear(Date(System.currentTimeMillis()))
    }

    @WorkerThread
    fun getDate(date:Date):List<MediaDate>{
        return mediaDateDao.getDate(date)
    }
}