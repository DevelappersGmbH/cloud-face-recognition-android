package de.develappers.facerecognition.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LogEntry(
    @ColumnInfo var timeStamp: String? = null,
    @ColumnInfo var kairos: String? = null,
    @ColumnInfo var amazon: String? = null,
    @ColumnInfo var microsoft: String? = null,
    @ColumnInfo var face: String? = null,
    @ColumnInfo var luxand: String? = null,
    @ColumnInfo var truth: String? = null
){

    @PrimaryKey (autoGenerate = true) var id: Int = 0
}