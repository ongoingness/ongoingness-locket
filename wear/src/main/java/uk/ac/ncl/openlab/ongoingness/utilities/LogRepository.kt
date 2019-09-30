package uk.ac.ncl.openlab.ongoingness.utilities

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData

class LogRepository(private val logDao: LogDao) {

    @WorkerThread
    suspend fun insert(log: Log) {
        logDao.insert(log);
    }

    @WorkerThread
    fun deleteAll() {
        logDao.deleteAll()
    }

    @WorkerThread
    fun getAll() : List<Log> {
        return logDao.getAll()
    }
    
}