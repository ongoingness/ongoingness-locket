package uk.ac.ncl.openlab.ongoingness.database

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.ac.ncl.openlab.ongoingness.database.repositories.LogRepository
import uk.ac.ncl.openlab.ongoingness.database.repositories.WatchMediaRepository
import uk.ac.ncl.openlab.ongoingness.database.schemas.WatchMedia
import uk.ac.ncl.openlab.ongoingness.utilities.deleteFile

class WatchMediaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WatchMediaRepository
    private val logRepository: LogRepository
    val allWatchMedia: LiveData<List<WatchMedia>>

    init {
        val watchMediaDao = WatchMediaRoomDatabase.getDatabase(application).watchMediaDao()
        repository = WatchMediaRepository(watchMediaDao)
        val logDao = WatchMediaRoomDatabase.getDatabase(application).logDao()
        logRepository = LogRepository(logDao)
        allWatchMedia = repository.allWatchMedia
    }

    fun insert(watchMedia: WatchMedia) = viewModelScope.launch(Dispatchers.IO){
        repository.insert(watchMedia)
    }

    fun delete(watchMedia: WatchMedia, context: Context) = viewModelScope.launch(Dispatchers.IO){
        deleteFile(context, watchMedia.path)
        repository.delete(watchMedia._id)
    }

}