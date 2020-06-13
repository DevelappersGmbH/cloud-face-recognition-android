package de.develappers.facerecognition.serviceAI

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.amazonaws.AmazonClientException
import com.amazonaws.services.rekognition.model.*
import com.amazonaws.util.IOUtils
import de.develappers.facerecognition.*
import de.develappers.facerecognition.database.model.entities.Visitor
import de.develappers.facerecognition.serviceAI.kairosServiceAI.model.*
import de.develappers.facerecognition.utils.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.Serializable
import java.nio.ByteBuffer


class KairosServiceAI(
    val context: Context,
    override var provider: String,
    override var isActive: Boolean): RecognitionService {

    val kairosApi = FaceApp.kairosApi

    override suspend fun train() {
        // does not need to be trained manually
    }

    override suspend fun deletePersonGroup(personGroupId: String) {
       removeGallery(personGroupId)
    }

    override suspend fun addPersonGroup(personGroupId: String) {
        //group is added automatically with first enrollment
    }


    override suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String, visitor: Visitor) {
        var imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        val imgString = ImageHelper.encodeImage(imgBitmap)
        if (imgString != null) {
            //step 1
            val outerId = imgUri.substringAfter("${context.getString(R.string.app_name)}/")
            enroll(imgString, outerId, personGroupId)
        }

    }

    override suspend fun addNewImage(personGroupId: String, imgUri: String, visitor: Visitor) {
        addNewVisitorToDatabase(personGroupId, imgUri, visitor)
    }

    override suspend fun identifyVisitor(personGroupId: String, imgUri: String): List<Any> {
        val imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        val imgString = ImageHelper.encodeImage(imgBitmap)
        val recogniseResponse = recognise(imgString!!, personGroupId) as RecogniseResponse
        val candidates: MutableList<KairosResult.KairosCandidate>  = mutableListOf()
        recogniseResponse.images.forEach{
            candidates.addAll(it.candidates)
        }
        return candidates

    }

    suspend fun recognise(imgBase64:String, personGroupId: String): Any =
        withContext(Dispatchers.IO) {
            try {
                val recogniseRequest = RecogniseRequest(imgBase64, personGroupId, CONFIDENCE_CANDIDATE, RETURN_RESULT_COUNT)
                kairosApi.recognise(recogniseRequest)
            } catch (e: Exception) {
                Log.d("Kairos recognise: ", e.toString())
            }
        }

    suspend fun enroll(imgBase64: String, outerId: String, personGroupId: String): Any =
        withContext(Dispatchers.IO) {
            try {
                val enrollRequest = EnrollRequest(imgBase64, outerId, personGroupId, true) //if set to true lets the API enroll every face found in your photo under the same subject_id
                val enrollResponse = kairosApi.enroll(enrollRequest)
                enrollResponse
            } catch (e: Exception) {
                Log.d("Kairos enroll: ", e.toString())
            }
        }


    suspend fun removeGallery(personGroupId: String) =
        withContext(Dispatchers.IO) {
            try {
                val response = kairosApi.removeGallery(GalleryRemoveRequest(personGroupId))
                Log.d("Kairos delete: ", response.toString())
            } catch (e: Exception) {
                Log.d("Kairos delete: ", e.toString())
            }
        }


    override fun setServiceId(visitor: Visitor, id: String){
        //should be an array, not used
        //user defined id in the service is set to the last imgPath, look up in the local db in imgpaths[]
        visitor.kairosId = id
    }


    override fun defineLocalIdPath(candidate: Any): String {
        candidate as KairosResult.KairosCandidate
        return "${FaceApp.galleryFolder}/${candidate.subject_id}"
    }

    override fun defineConfidenceLevel(candidate: Any): Double {
        candidate as KairosResult.KairosCandidate
        return candidate.confidence
    }
}