package de.develappers.facerecognition.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.develappers.facerecognition.database.model.Company

@Dao
interface CompanyDao {
    @Query("SELECT * FROM company")
    fun getAll(): List<Company>

    @Query("SELECT * FROM company WHERE companyId IN (:companyIds)")
    fun loadAllByIds(companyIds: IntArray): List<Company>

    @Query("SELECT * FROM company WHERE company_name LIKE :companyName " +
            "LIMIT 1")
    fun findByName(companyName: String): Company

    @Insert
    fun insertAll(vararg companies: Company)

    @Delete
    fun delete(company: Company)
}