package uk.ac.ncl.openlab.ongoingness.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.ac.ncl.openlab.ongoingness.database.schemas.Log

/**
 * Data access object for Logs.
 *
 * @author Luis Carvalho
 */
@Dao
interface LogDao {

    /**
     * Get all logs from the database.
     *
     * @return list of all logs.
     */
    @Query("SELECT * from log ORDER BY `timestamp` ASC")
    fun getAll(): List<Log>

    /**
     * Added a new log to the database.
     *
     * @param log the new logs to be added.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: Log)

    /**
     * Deletes all logs from the database.
     */
    @Query("DELETE FROM log")
    fun deleteAll()

}