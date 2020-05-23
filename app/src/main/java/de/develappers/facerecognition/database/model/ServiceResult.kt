package de.develappers.facerecognition.database.model

import java.io.Serializable

class ServiceResult(var provider: String,
                    var identificationTime: Long,
                    var confidence: Double): Serializable {

}