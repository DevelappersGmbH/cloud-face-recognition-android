package de.develappers.facerecognition.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import de.develappers.facerecognition.database.model.entities.LogEntry

@Dao
interface LogDao {
    @Query("SELECT * FROM logEntry")
    fun getAll(): List<LogEntry>

    @Query("SELECT * FROM logEntry WHERE id IN (:logIds)")
    fun loadAllByIds(logIds: IntArray): List<LogEntry>

    @Insert
    fun insertAll(vararg logEntries: LogEntry)

}