package de.develappers.facerecognition.database.model.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
data class Company(
    @ColumnInfo (name = "company_name") var companyName: String?
): Serializable
{

    @PrimaryKey (autoGenerate = true) var companyId: Int = 0
}