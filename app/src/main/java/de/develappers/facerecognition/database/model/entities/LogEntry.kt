package de.develappers.facerecognition.database.model.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.develappers.facerecognition.database.model.ServiceResult

@Entity
data class LogEntry(
    @ColumnInfo var timeStamp: String? = null,
    @ColumnInfo var serviceResults: List<ServiceResult>? = null,
    @ColumnInfo var trueVisitorId: String? = null
){

    @PrimaryKey (autoGenerate = true) var id: Int = 0
}