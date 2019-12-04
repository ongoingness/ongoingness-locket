package uk.ac.ncl.openlab.ongoingness.database.repositories

import androidx.annotation.WorkerThread
import uk.ac.ncl.openlab.ongoingness.database.daos.WatchMediaDao
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import java.sql.Date

class WatchMediaRepository(private val watchMediaDao: WatchMediaDao) {

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
    fun getCollection(collection: String): List<WatchMedia> {
        return watchMediaDao.getCollection(collection)
    }

    @WorkerThread
    fun getMediaWithDate(date: Date):List<WatchMedia>{
        return watchMediaDao.getForDayOfYear(date)
    }

    @WorkerThread
    fun getMediaWithNoDates():List<WatchMedia>{
        return watchMediaDao.getMediaWithNoDates()
    }
}