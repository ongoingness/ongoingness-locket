package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

class WatchMediaRepository(private val watchMediaDao: WatchMediaDao) {

    val allWatchMedia: LiveData<List<WatchMedia>> = watchMediaDao.getAllMedia()

    @WorkerThread
    suspend fun insert(watchMedia: WatchMedia) {
        watchMediaDao.insert(watchMedia);
    }

    @WorkerThread
    suspend fun get(id: String) : WatchMedia {
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
    suspend fun getAll() : List<WatchMedia> {





        return watchMediaDao.getAll()
    }
}