package uk.ac.ncl.openlab.ongoingness.database.repositories

import androidx.annotation.WorkerThread
import uk.ac.ncl.openlab.ongoingness.database.daos.LogDao
import uk.ac.ncl.openlab.ongoingness.database.schemas.Log

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