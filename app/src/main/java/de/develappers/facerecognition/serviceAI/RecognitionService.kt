package de.develappers.facerecognition.serviceAI

import com.microsoft.projectoxford.face.contract.Candidate
import com.microsoft.projectoxford.face.contract.CreatePersonResult
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.Visitor

interface RecognitionService {
    var isActive: Boolean
    suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String, visitor: Visitor)
    suspend fun identifyVisitor(personGroupId: String, imgUri: String): List<Any>
    suspend fun addPersonGroup(personGroupId: String)
    suspend fun deletePersonGroup(personGroupId: String)
    suspend fun train()
    fun setServiceId(visitor: Visitor, id: String)
    fun setConfidenceLevel(candidate: Any, recognisedCandidate: RecognisedCandidate)
    fun defineLocalIdPath(candidate: Any): String
    fun defineConfidenceLevel(candidate: Any): Double
}