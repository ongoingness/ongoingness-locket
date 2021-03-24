package uk.ac.ncl.openlab.ongoingness.database.repositories

import androidx.annotation.WorkerThread
import uk.ac.ncl.openlab.ongoingness.database.daos.WatchMediaDao
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia

/**
 * Repository for WatchMedia.
 *
 * @param watchMediaDao Data access object to WatchMedia.
 *
 * @author Luis Carvalho
 */
class WatchMediaRepository(private val watchMediaDao: WatchMediaDao) {

    /**
     * Insert a new WatchMedia into the database.
     *
     * @param watchMedia WatchMedia to be added.
     */
    @WorkerThread
    suspend fun insert(watchMedia: WatchMedia) {
        watchMediaDao.insert(watchMedia)
    }

    /**
     * Get a WatchMedia given its id.
     *
     * @param id id of the WatchMedia.
     * @return WatchMedia with the given id.
     */
    @WorkerThread
    fun get(id: String) : WatchMedia {
        return watchMediaDao.getMedia(id)
    }

    /**
     * Delete a WatchMedia given its id.
     *
     * @param id id of the WatchMedia to be deleted.
     */
    @WorkerThread
    suspend fun delete(id: String) {
        watchMediaDao.delete(id)
    }

    /**
     * Deletes all WatchMedia from the database.
     */
    @WorkerThread
    fun deleteAll() {
        watchMediaDao.deleteAll()
    }

    /**
     * Get all WatchMedia from the database.
     *
     * @return list with all WatchMedia.
     */
    @WorkerThread
    fun getAll() : List<WatchMedia> {
        return watchMediaDao.getAll()
    }

    /**
     * Get a WatchMedia belonging to a collection.
     *
     * @param collection name of the collection.
     * @return list of WatchMedia belonging to the collection.
     */
    @WorkerThread
    fun getCollection(collection: String): List<WatchMedia> {
        return watchMediaDao.getCollection(collection)
    }

    /**
     * Get WatchMedia associated to a date.
     *
     * @param day day of the month.
     * @param month number of the month in a year.
     * @return list of WatchMedia associated to the date.
     */
    @WorkerThread
    fun getMediaWithDate(day: Int, month: Int):List<WatchMedia>{
        return watchMediaDao.getMediaForDayOfMonth(day, month)
    }

    /**
     * Get WatchMedia with no dates associated.
     *
     * @return list of all WatchMedia with no WatchMedia.
     */
    @WorkerThread
    fun getMediaWithNoDates():List<WatchMedia>{
        return watchMediaDao.getMediaWithNoDates()
    }
}