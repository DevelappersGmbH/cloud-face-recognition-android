package de.develappers.facerecognition.serviceAI

import android.content.Context
import android.net.Uri
import android.util.Log
import com.microsoft.projectoxford.face.contract.CreatePersonResult
import de.develappers.facerecognition.*
import de.develappers.facerecognition.database.model.entities.Visitor
import de.develappers.facerecognition.retrofit.FaceApi
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.*
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.AddFaceToPersonResponse
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.CreatePersonResponse
import de.develappers.facerecognition.utils.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable


class LuxandServiceAI(
    val context: Context,
    override var provider: String,
    override var isActive: Boolean): RecognitionService {

    val luxandApi = FaceApp.luxandApi

    override suspend fun train() {
        // does not need to be trained manually
    }

    override suspend fun deletePersonGroup(personGroupId: String) {
        // does not have face sets
    }

    //step 1
    override suspend fun addPersonGroup(personGroupId: String) {
        // does not have face sets
    }

    override suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String, visitor: Visitor) {
        //step 2
        val createPersonResponse = addPersonToGroup(personGroupId)
        luxandAddFaceToPerson()
        setServiceId(visitor, createPersonResult.personId.toString())

    }

    override suspend fun addNewImage(personGroupId: String, imgUri: String, visitor: Visitor) {
        addNewVisitorToDatabase(personGroupId, imgUri, visitor)
    }

    override suspend fun identifyVisitor(personGroupId: String, imgUri: String): List<Any> {
        var imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        val imgString = ImageHelper.encodeImage(imgBitmap)
        val searchFaceResponse = faceSearch(imgString) as SearchResponse
        return searchFaceResponse.results as List<Any>;

    }

    suspend fun addPersonToGroup(personName: String): CreatePersonResponse {
        return luxandCreatePerson(personName)
    }

    suspend fun luxandCreatePerson(personName: String): CreatePersonResponse =
        withContext(Dispatchers.IO) {
            luxandApi.createPerson(personName)
        }


    suspend fun luxandAddFaceToPerson(luxandId: String?, photoFile: String?): Any?  =
        withContext(Dispatchers.IO) {
            var result : AddFaceToPersonResponse? = null
            try {
                result = luxandApi.addFaceToPerson(luxandId, photoFile)
                result
            } catch (e: Exception) {
                Log.d("Luxand addFaceToPerson: ", e.toString())
            }
        }

    suspend fun luxandFaceSearch(photoFile: String?): Any? =
        withContext(Dispatchers.IO) {
            try {
                luxandApi.search(photoFile)
            } catch (e: Exception) {
                Log.d("Luxand search: ", e.toString())
            }
        }


    override fun setServiceId(visitor: Visitor, id: String){
        //should be an array, not used
        //user defined id in the service is set to the last imgPath, look up in the local db in imgpaths[]
        visitor.faceId = id
    }


    override fun defineLocalIdPath(candidate: Any): String {
        candidate as SearchFaceResult
        return "${FaceApp.galleryFolder}/${candidate.user_id}"
    }

    override fun defineConfidenceLevel(candidate: Any): Double {
        candidate as SearchFaceResult
        return candidate.confidence / 100.0
    }
}