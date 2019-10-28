package uk.ac.ncl.openlab.ongoingness.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.ac.ncl.openlab.ongoingness.database.schemas.Log

@Dao
interface LogDao {

    @Query("SELECT * from log ORDER BY `timestamp` ASC")
    fun getAll(): List<Log>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: Log)

    @Query("DELETE FROM log")
    fun deleteAll()

}