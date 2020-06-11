package de.develappers.facerecognition.serviceAI.kairosServiceAI.model

class KairosFace(
    val attributes: KairosAttributes,
    val transaction: KairosTransaction
) {
    class KairosAttributes(
        val lips: String,
        val asian: Double,
        val gender: KairosGender,
        val age: Int,
        val hispanic: Double,
        val other: Double,
        val black: Double,
        val white: Double,
        val glasses: String
    )

    class KairosGender(val type: String)

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
        val face_id: String
    )
}
