package uk.ac.ncl.openlab.ongoingness.database

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.ncl.openlab.ongoingness.database.repositories.LogRepository
import uk.ac.ncl.openlab.ongoingness.database.repositories.WatchMediaRepository
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import uk.ac.ncl.openlab.ongoingness.utilities.deleteFile

/**
 * View model giving access to the local database.
 *
 * @param application the current application.
 * @author Luis Carvalho
 */
class WatchMediaViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Access point to the local database tables with the media.
     */
    private val repository: WatchMediaRepository

    /**
     * Access point to the local database table with the logs
     */
    private val logRepository: LogRepository

    init {
        val watchMediaDao = WatchMediaRoomDatabase.getDatabase(application).watchMediaDao()
        repository = WatchMediaRepository(watchMediaDao)
        val logDao = WatchMediaRoomDatabase.getDatabase(application).logDao()
        logRepository = LogRepository(logDao)
    }

    /**
     * Added a new WatchMedia element to the database.
     *
     * @param watchMedia media item to be added to the database.
     */
    fun insert(watchMedia: WatchMedia) = viewModelScope.launch(Dispatchers.IO){
        repository.insert(watchMedia)
    }

    /**
     * Delete an existing WatchMedia element from the database and associated files.
     */
    fun delete(watchMedia: WatchMedia, context: Context) = viewModelScope.launch(Dispatchers.IO){
        deleteFile(context, watchMedia.path)
        repository.delete(watchMedia._id)
    }

    /**
     * Get all WatchMedia in the database.
     *
     * @return a list of all WatchMedia in the local database.
     */
    fun allWatchMedia():List<WatchMedia>{
        return repository.getAll()
    }

    /**
     * Get all WatchMedia belonging to a given collection.
     *
     * @param collection the name of the collection.
     * @return a list with the WatchMedia belonging to that collection.
     */
    fun getCollection(collection: String): List<WatchMedia> {
        return repository.getCollection(collection);
    }

    /**
     * Get a list of WatchMedia associated to a day of the month and a month.
     *
     * @param day day of a month.
     * @param month number representation of a month.
     * @return a list with WatchMedia associated to that date.
     */
    fun getWatchMediaForDate(day: Int, month: Int): List<WatchMedia>{
        return repository.getMediaWithDate(day, month)
    }

    /**
     * Get a list of WatchMedia with no dates associated to.
     *
     * @return a list of WatchMedia with no dates associated.
     */
    fun getWatchMediaWithNoDate():List<WatchMedia> {
        return repository.getMediaWithNoDates()
    }

}