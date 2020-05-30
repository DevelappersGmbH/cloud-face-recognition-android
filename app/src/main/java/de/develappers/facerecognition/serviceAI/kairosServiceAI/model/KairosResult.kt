package de.develappers.facerecognition.serviceAI.kairosServiceAI.model

import de.develappers.facerecognition.database.model.RecognisedCandidate

class KairosResult(
    val transaction: KairosTransaction,
    val candidates: List<KairosCandidate>
) {
    class KairosCandidate(
        val subject_id: String,
        val confidence: Double,
        val enrollment_timestamp: Long
    )

    class KairosTransaction(
        val status: String,
        val topLeftX: Int,
        val topLeftY: Int,
        val gallery_name: String,
        val timestamp: Long,
        val height: Int,
        val quality: Double,
        val confidence: Double,
        val subject_id: String,
        val face_id: Int
    )
}