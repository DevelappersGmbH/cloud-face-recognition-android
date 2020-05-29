package de.develappers.facerecognition.serviceAI

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.amazonaws.AmazonClientException
import com.amazonaws.services.rekognition.model.*
import com.amazonaws.util.IOUtils
import de.develappers.facerecognition.FaceApp
import de.develappers.facerecognition.R
import de.develappers.facerecognition.database.model.entities.Visitor
import de.develappers.facerecognition.VISITORS_GROUP_DESCRIPTION
import de.develappers.facerecognition.VISITORS_GROUP_ID
import de.develappers.facerecognition.VISITORS_GROUP_NAME
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

    val amazonServiceClient = FaceApp.amazonServiceClient
    val personGroupId = VISITORS_GROUP_ID
    val personGroupName = VISITORS_GROUP_NAME
    val personGroupDescription = VISITORS_GROUP_DESCRIPTION

    override suspend fun train() {
        // does not need to be trained manually
    }

    override suspend fun deletePersonGroup(personGroupId: String) {
       amazonDeletePersonGroup(personGroupId)
    }

    //step 1
    override suspend fun addPersonGroup(personGroupId: String) {
        amazonAddGroup(personGroupId)
    }

    override suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String, visitor: Visitor) {
        //step 2
        val imageInputStream: InputStream = convertBitmapToStream(imgUri)
        val indexFacesResult = amazonIndexFaces(imageInputStream, imgUri) as IndexFacesResult
        val faceIds = mutableListOf<String>()
        indexFacesResult.faceRecords.forEach{
            faceIds.add(it.face.faceId)
        }
        //add faceIds to amazonIds
    }

    override suspend fun addNewImage(personGroupId: String, imgUri: String, visitor: Visitor) {
        addNewVisitorToDatabase(personGroupId, imgUri, visitor)
    }

    override suspend fun identifyVisitor(personGroupId: String, imgUri: String): List<Any> {
        val imageInputStream: InputStream = convertBitmapToStream(imgUri)
        val faceSearchResult = amazonIdentifyVisitor(imageInputStream, 0.0f) as SearchFacesByImageResult
        return faceSearchResult.faceMatches;

    }


    suspend fun amazonIdentifyVisitor(imageInputStream: InputStream, confidenceThreshold: Float): Serializable? =
        withContext(Dispatchers.IO) {
            try {
                val imgBytes = ByteBuffer.wrap(IOUtils.toByteArray(imageInputStream))
                val image = Image().withBytes(imgBytes)

                val searchFacesByImageRequest = SearchFacesByImageRequest()
                    .withCollectionId(personGroupId)
                    .withImage(image)
                    .withFaceMatchThreshold(confidenceThreshold)

                amazonServiceClient?.searchFacesByImage(searchFacesByImageRequest)
            } catch (e: AmazonClientException) {
                Log.d("Amazon", e.message!!)
            }
        }

    suspend fun amazonAddGroup(personGroupId: String): Serializable? =
        withContext(Dispatchers.IO) {
            try {
                val request = CreateCollectionRequest().withCollectionId(personGroupId)
                amazonServiceClient?.createCollection(request)
            } catch (e: AmazonClientException) {
                Log.d("Amazon", e.message!!)
            }
        }


    suspend fun amazonIndexFaces(imageInputStream: InputStream, imgUri: String): Serializable? =
        withContext(Dispatchers.IO) {
            try {
                val imgBytes = ByteBuffer.wrap(IOUtils.toByteArray(imageInputStream))
                val image = Image().withBytes(imgBytes)

                val indexFacesRequest = IndexFacesRequest()
                    .withImage(image).withCollectionId(personGroupId).withExternalImageId(imgUri.substringAfter("${context.getString(
                        R.string.app_name)}/"))
                val indexFacesResult = amazonServiceClient?.indexFaces(indexFacesRequest)
                val unindexedFaces = indexFacesResult?.getUnindexedFaces()
                println("Faces not indexed:")
                unindexedFaces?.forEach {face->
                    println("  Location:" + face.faceDetail.boundingBox.toString())
                    println("  Reasons:")
                    face.reasons.forEach {reason->
                        println(reason)
                    }
                }
                indexFacesResult
            } catch (e: AmazonClientException) {
                Log.d("Amazon", e.message!!)
            }
        }

    suspend fun amazonDeletePersonGroup(personGroupId: String) =
        withContext(Dispatchers.IO) {
            try {
                val request = DeleteCollectionRequest()
                    .withCollectionId(personGroupId)
                amazonServiceClient?.deleteCollection(request)
            } catch (e: AmazonClientException) {
                Log.d("Amazon", e.message!!)
            }
        }

    fun convertBitmapToStream(imgUri: String): InputStream{
        var imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        val stream = ByteArrayOutputStream()
        imgBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return ByteArrayInputStream(stream.toByteArray())
    }

    override fun setServiceId(visitor: Visitor, id: String){
        visitor.amazonId = id
    }


    override fun defineLocalIdPath(candidate: Any): String {
        candidate as FaceMatch
        return "${FaceApp.galleryFolder}/${candidate.face.externalImageId}"
    }

    override fun defineConfidenceLevel(candidate: Any): Double {
        candidate as FaceMatch
        return candidate.similarity / 100.0
    }
}