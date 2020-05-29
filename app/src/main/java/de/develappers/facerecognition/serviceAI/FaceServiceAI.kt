package de.develappers.facerecognition.serviceAI

import android.content.Context
import android.net.Uri
import android.util.Log
import de.develappers.facerecognition.*
import de.develappers.facerecognition.database.model.entities.Visitor
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.*
import de.develappers.facerecognition.utils.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Serializable


class FaceServiceAI(
    val context: Context,
    override var provider: String,
    override var isActive: Boolean): RecognitionService {

    val apiKey = BuildConfig.FACE_KEY
    val apiSecret = BuildConfig.FACE_SECRET

    val faceApi= FaceApp.faceServiceClient

    override suspend fun train() {
        // does not need to be trained manually
    }

    override suspend fun deletePersonGroup(personGroupId: String) {
            faceDeleteFaceSet(personGroupId)
    }

    //step 1
    override suspend fun addPersonGroup(personGroupId: String) {
        faceCreateFaceSet(personGroupId)
    }

    override suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String, visitor: Visitor) {
        //step 2
        var imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        val imgString = ImageHelper.encodeImage(imgBitmap)
        var faces: List<Face>? = listOf()
        if (imgString != null) {
            //step 3
            val result = faceDetect(imgString) as DetectResponse
            faces = result.faces
        }
        faces?.forEach {
            //step 4 (given there is only one person/face in the captured photo and we can add all detected faces to this person)
           faceSetUserId(it.face_token!!, imgUri.substringAfter("${context.getString(R.string.app_name)}/"))
            addFaceToFaceSet(it.face_token!!, personGroupId)
        }

    }

    override suspend fun identifyVisitor(personGroupId: String, imgUri: String): List<Any> {
        var imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        val imgString = ImageHelper.encodeImage(imgBitmap)
        val searchFaceResponse = faceSearch(imgString, personGroupId) as SearchResponse
        return searchFaceResponse.results as List<Any>;

    }

    suspend fun faceSetUserId(faceToken: String?, localPathId: String?) =
        withContext(Dispatchers.IO) {
            try {
                 faceApi?.setUserId(apiKey, apiSecret, faceToken, localPathId)
            } catch (e: Exception) {
                Log.d("Face++ setuserid: ", e.toString())
            }
        }

    suspend fun addFaceToFaceSet(faceToken: String?, personGroupId: String?) =
        withContext(Dispatchers.IO) {
            var result : FaceSetCreateResponse? = null
            try {
                result = faceApi?.addFaceToFaceSet(apiKey, apiSecret, personGroupId, faceToken)
            } catch (e: Exception) {
                Log.d("Face++ addfacetofs: ", e.toString())
            }
        }

    suspend fun faceSearch(imgBase64:String?, personGroupId: String?): Serializable? =
        withContext(Dispatchers.IO) {
            try {
                faceApi?.search(apiKey, apiSecret, imgBase64, personGroupId, RETURN_RESULT_COUNT)
            } catch (e: Exception) {
                Log.d("Face++ search: ", e.toString())
            }
        }

    suspend fun faceDetect(imgBase64: String?): Serializable? =
        withContext(Dispatchers.IO) {
            try {
                val detectFacesResult = faceApi?.detect(apiKey, apiSecret, imgBase64)
                detectFacesResult
            } catch (e: Exception) {
                Log.d("Face++ detect: ", e.toString())
            }
        }

    suspend fun faceCreateFaceSet(personGroupId: String?): Serializable? =
        withContext(Dispatchers.IO) {
            try {
                val response = faceApi?.createFaceSet(apiKey, apiSecret, personGroupId)
                Log.d("Face++", response?.faceset_token.toString())
            } catch (e: Exception) {
                Log.d("Face++ create fs: ", e.toString())
            }
        }



    suspend fun faceDeleteFaceSet(personGroupId: String?) =
        withContext(Dispatchers.IO) {
            try {
                faceApi?.removeFaceTokensFromSet(apiKey, apiSecret, personGroupId, REMOVE_ALL_TOKENS)
                val response = faceApi?.deleteFaceSet(apiKey, apiSecret, personGroupId)
                Log.d("Face++", response?.faceset_token.toString())
            } catch (e: Exception) {
                Log.d("Face++ delete: ", e.toString())
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