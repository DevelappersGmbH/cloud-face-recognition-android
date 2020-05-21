package de.develappers.facerecognition.serviceAI

import com.microsoft.projectoxford.face.contract.Candidate
import com.microsoft.projectoxford.face.contract.CreatePersonResult
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.Visitor

interface RecognitionService {
    suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String, visitor: Visitor)
    fun setServiceId(visitor: Visitor, id: String)
    fun setConfidenceLevel(candidate: Any, recognisedCandidate: RecognisedCandidate)
    fun defineLocalIdPath(candidate: Any): String
    fun defineConfidenceLevel(candidate: Any): Double
}