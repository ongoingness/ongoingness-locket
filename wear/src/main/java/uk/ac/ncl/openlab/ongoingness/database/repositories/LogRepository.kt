package uk.ac.ncl.openlab.ongoingness.database.repositories

import androidx.annotation.WorkerThread
import uk.ac.ncl.openlab.ongoingness.database.daos.LogDao
import uk.ac.ncl.openlab.ongoingness.database.schemas.Log

/**
 * Repository for Logs.
 *
 * @param logDao Repository for Logs.
 *
 * @author Luis Carvalho
 */
class LogRepository(private val logDao: LogDao) {

    /**
     * Added a new log to the database.
     *
     * @param log the new logs to be added.
     */
    @WorkerThread
    suspend fun insert(log: Log) {
        logDao.insert(log)
    }

    /**
     * Deletes all logs from the database.
     */
    @WorkerThread
    fun deleteAll() {
        logDao.deleteAll()
    }

    /**
     * Get all logs from the database.
     *
     * @return list of all logs.
     */
    @WorkerThread
    fun getAll() : List<Log> {
        return logDao.getAll()
    }
    
}