package de.develappers.facerecognition.serviceAI

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.amazonaws.AmazonClientException
import com.amazonaws.services.rekognition.model.*
import com.amazonaws.util.IOUtils
import com.microsoft.projectoxford.face.rest.ClientException
import de.develappers.facerecognition.FaceApp
import de.develappers.facerecognition.R
import de.develappers.facerecognition.utils.VISITORS_GROUP_DESCRIPTION
import de.develappers.facerecognition.utils.VISITORS_GROUP_ID
import de.develappers.facerecognition.utils.VISITORS_GROUP_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.Serializable
import java.nio.ByteBuffer


class AmazonServiceAI(val context: Context) {
    val amazonServiceClient = FaceApp.amazonServiceClient
    val personGroupId = VISITORS_GROUP_ID
    val personGroupName = VISITORS_GROUP_NAME
    val personGroupDescription = VISITORS_GROUP_DESCRIPTION

    //step 1
    suspend fun addPersonGroup() {
        amazonAddGroup(personGroupId)
    }

    suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String): List<String> {
        //step 2
        val imageInputStream: InputStream = convertBitmapToStream(imgUri)
        val indexFacesResult = amazonIndexFaces(imageInputStream, imgUri) as IndexFacesResult
        val faceIds = mutableListOf<String>()
        indexFacesResult.faceRecords.forEach{
            faceIds.add(it.face.faceId)
        }
        return faceIds
    }

    suspend fun identifyVisitor(personGroupId: String, imgUri: String): List<FaceMatch> {
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
}