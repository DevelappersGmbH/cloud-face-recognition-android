package de.develappers.facerecognition.serviceAI

import android.content.Context
import android.util.Log
import de.develappers.facerecognition.CONFIDENCE_CANDIDATE
import de.develappers.facerecognition.FaceApp
import de.develappers.facerecognition.FaceApp.Companion.galleryFolder
import de.develappers.facerecognition.R
import de.develappers.facerecognition.database.model.entities.Visitor
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.AddFaceToPersonResponse
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.CreatePersonResponse
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.LuxandFace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File


class LuxandServiceAI(
    val context: Context,
    override var provider: String,
    override var isActive: Boolean
) : RecognitionService {

    val luxandApi = FaceApp.luxandApi

    override suspend fun train() {
        // does not need to be trained manually
    }

    override suspend fun deletePersonGroup(personGroupId: String) {
        val persons = luxandApi.listPersons()
        persons.forEach{
            luxandApi.deletePerson(it.id)
        }
    }

    //step 1
    override suspend fun addPersonGroup(personGroupId: String) {
        // does not have face sets
    }

    override suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String, visitor: Visitor) {
        //step 2
        val createPersonResponse = addPersonToGroup(visitor.visitorId.toString())
        setServiceId(visitor, createPersonResponse.id.toString())
        addNewImage(personGroupId, imgUri, visitor)

    }

    override suspend fun addNewImage(personGroupId: String, imgUri: String, visitor: Visitor) {
        val body = createBodyPart(imgUri)
        luxandAddFaceToPerson(visitor.luxandId.toInt(), body)
    }

    fun createBodyPart(imgUri: String): MultipartBody.Part{
        val photoFile = File(galleryFolder, imgUri.substringAfter("${context.getString(R.string.app_name)}/"))

        val body = MultipartBody.Part.createFormData(
            "photo", photoFile.name, RequestBody.create(
                MediaType.parse("multipart/form-data"),
                photoFile
            )
        )
        return body
    }

    override suspend fun identifyVisitor(personGroupId: String, imgUri: String): List<Any> {
        val imageBody = createBodyPart(imgUri)
        val requestThreshold: RequestBody = RequestBody.create(
            MediaType.parse("multipart/form-data"), CONFIDENCE_CANDIDATE.toString()
        )
        val searchFaceResponse = luxandFaceSearch(imageBody, requestThreshold) as List<Any>

        return searchFaceResponse

    }

    suspend fun addPersonToGroup(personName: String): CreatePersonResponse {
        return luxandCreatePerson(personName)
    }

    suspend fun luxandCreatePerson(personName: String): CreatePersonResponse =
        withContext(Dispatchers.IO) {
            luxandApi.createPerson(personName)
        }


    suspend fun luxandAddFaceToPerson(luxandId: Int?, photoFile: MultipartBody.Part): Any? =
        withContext(Dispatchers.IO) {
            var result: AddFaceToPersonResponse? = null
            try {
                result = luxandApi.addFaceToPerson(luxandId, photoFile)
                result
            } catch (e: Exception) {
                Log.d("Luxand addFaceToPerson: ", e.toString())
            }
        }

    suspend fun luxandFaceSearch(photoFile: MultipartBody.Part, requestThreshold: RequestBody): Any =
        withContext(Dispatchers.IO) {
            var result: List<LuxandFace> = mutableListOf()
            try {
                result = luxandApi.search(photoFile, requestThreshold)
                result
            } catch (e: Exception) {
                Log.d("Luxand search: ", e.toString())
            }
        }


    override fun setServiceId(visitor: Visitor, id: String) {
        //should be an array, not used
        //user defined id in the service is set to the last imgPath, look up in the local db in imgpaths[]
        visitor.luxandId = id
    }

    override fun defineLocalIdPath(candidate: Any): String {
        candidate as LuxandFace
        return candidate.id.toString()
    }

    override fun defineConfidenceLevel(candidate: Any): Double {
        candidate as LuxandFace
        return candidate.probability
    }
}