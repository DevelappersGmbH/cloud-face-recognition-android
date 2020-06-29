package de.develappers.facerecognition.database.model.entities

import androidx.room.ColumnInfo
import de.develappers.facerecognition.database.model.ServiceResult
import java.io.Serializable

data class AnalysisData(
    val recognised_id: Long? = 0L,
    val first_name: String? = "",
    val last_name: String? = "",
    val provider: String? = "",
    val confidence: Double? = 0.0,
    val identificationTime: Long? = 0L,
    val true_id: Long,
    val run_count: Int
)