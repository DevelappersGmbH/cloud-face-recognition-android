package de.develappers.facerecognition.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LogEntry(
    @ColumnInfo var timeStamp: String? = null,
    @ColumnInfo var serviceResults: List<ServiceResult>? = null,
    @ColumnInfo var trueVisitorId: String? = null
){

    @PrimaryKey (autoGenerate = true) var id: Int = 0
}