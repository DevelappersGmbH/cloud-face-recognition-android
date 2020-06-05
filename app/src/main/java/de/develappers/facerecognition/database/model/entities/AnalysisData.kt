package de.develappers.facerecognition.database.model.entities

import androidx.room.ColumnInfo
import de.develappers.facerecognition.database.model.ServiceResult
import java.io.Serializable

data class AnalysisData(
    val true_id: Long? = 0L,
    val first_name: String? = "",
    val last_name: String? = "",
    val microsoftId: String? = "",
    val amazonId: String? = "",
    val luxandId: String? = "",
    val faceId: String? = "",
    val kairosId: String? = "",
    val provider: String? = "",
    val confidence: Double? = 0.0,
    val identificationTime: Long? = 0L,
    val known: Boolean,
    val run_count: Int
)