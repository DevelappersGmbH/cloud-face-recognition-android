package de.develappers.facerecognition.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.develappers.facerecognition.database.model.Visitor
import java.util.*

@Dao
interface VisitorDao {
    @Query("SELECT * FROM visitor")
    fun getAll(): LiveData<List<Visitor>>

    @Query("SELECT * FROM visitor")
    fun getAllVis(): List<Visitor>

    @Query("SELECT * FROM visitor ORDER BY RANDOM() LIMIT 1")
    fun getRandomVisitor(): LiveData<Visitor>

    @Query("SELECT * FROM visitor WHERE microsoft_id IN (:visitorIds)")
    suspend fun findAllMicrosoftByIds(visitorIds: MutableList<String>): List<Visitor>

    @Query("SELECT * FROM visitor WHERE first_name LIKE :first AND " +
            "last_name LIKE :last LIMIT 1")
    suspend fun findByName(first: String, last: String): Visitor

    @Query("SELECT * FROM visitor WHERE microsoft_id = :id")
    suspend fun findByMicrosoftId(id: String): Visitor

    @Insert
    suspend fun insertAll(vararg visitors: Visitor)

    @Insert
    suspend fun insert(visitor: Visitor): Long

    @Delete
    suspend fun delete(visitor: Visitor)

    @Query("DELETE FROM visitor")
    suspend fun deleteAll()
}