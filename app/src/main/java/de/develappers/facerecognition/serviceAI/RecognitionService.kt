package de.develappers.facerecognition.serviceAI

import de.develappers.facerecognition.database.model.entities.Visitor

interface RecognitionService {
    var isActive: Boolean
    var provider: String
    suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String, visitor: Visitor)
    suspend fun identifyVisitor(personGroupId: String, imgUri: String): List<Any>
    suspend fun addPersonGroup(personGroupId: String)
    suspend fun deletePersonGroup(personGroupId: String)
    suspend fun addNewImage(personGroupId: String, imgUri: String, visitor: Visitor)
    suspend fun train()
    fun setServiceId(visitor: Visitor, id: String)
    fun defineLocalIdPath(candidate: Any): String
    fun defineConfidenceLevel(candidate: Any): Double
}