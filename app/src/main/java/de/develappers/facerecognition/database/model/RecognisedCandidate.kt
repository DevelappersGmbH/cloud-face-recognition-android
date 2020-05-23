package de.develappers.facerecognition.database.model

import java.io.Serializable

class RecognisedCandidate(): Serializable {
    lateinit var visitor: Visitor
    var serviceResults = mutableListOf<ServiceResult>()
}
