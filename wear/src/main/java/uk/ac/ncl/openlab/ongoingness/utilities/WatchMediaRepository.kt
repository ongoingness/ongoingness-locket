package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import java.sql.Date

class WatchMediaRepository(private val watchMediaDao: WatchMediaDao) {

    val allWatchMedia: LiveData<List<WatchMedia>> = watchMediaDao.getAllMedia()

    @WorkerThread
    suspend fun insert(watchMedia: WatchMedia) {
        watchMediaDao.insert(watchMedia)
    }

    @WorkerThread
    fun get(id: String) : WatchMedia {
        return watchMediaDao.getMedia(id)
    }

    @WorkerThread
    suspend fun delete(id: String) {
        watchMediaDao.delete(id)
    }

    @WorkerThread
    fun deleteAll() {
        watchMediaDao.deleteAll()
    }

    @WorkerThread
    fun getAll() : List<WatchMedia> {
        return watchMediaDao.getAll()
    }


    @WorkerThread
    fun getSignificant(date: Date):List<WatchMedia>{
        return watchMediaDao.getSignificant(date)
    }
}