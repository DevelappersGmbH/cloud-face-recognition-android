package de.develappers.facerecognition.serviceAI

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.microsoft.projectoxford.face.contract.*
import com.microsoft.projectoxford.face.rest.ClientException
import de.develappers.facerecognition.FaceApp
import de.develappers.facerecognition.utils.VISITORS_GROUP_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

class MicrosoftServiceAI(val context: Context) {

    // Get an instance of face service client.
    val faceServiceClient = FaceApp.faceServiceClient
    //personGroupId = 1 for visitors
    val personGroupId = VISITORS_GROUP_ID
    val personGroupName = "visitors"
    val personGroupDescription = "all visitors"

    //step 1
    suspend fun addPersonGroup() {
        microsoftAddGroup(personGroupId, personGroupName, personGroupDescription)
    }

    suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String): String {
        val userData = "user data"
        //step 2
        val createPersonResult = addPersonToGroup(personGroupId, imgUri)
        Log.d("Retrieving", imgUri)
        var imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        var faces = arrayOf<Face>()
        if (imgBitmap != null) {
            //step 3
            faces = detectFacesInImage(imgBitmap)
        }
        faces.forEach {
            val stream = ByteArrayOutputStream()
            imgBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val imageInputStream: InputStream = ByteArrayInputStream(stream.toByteArray())
            //step 4 (given there is only one person/face in the captured photo and we can add all detected faces to this person)
            microsoftAddFaceToPerson(
                personGroupId,
                createPersonResult.personId,
                imageInputStream,
                userData,
                it.faceRectangle
            )
        }

        return createPersonResult.personId.toString()
    }

    suspend fun identifyVisitor(personGroupId: String, imgUri: String): Array<IdentifyResult> {
        val faceIds: MutableList<UUID> = mutableListOf()
        val imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        var faces = arrayOf<Face>()
        if (imgBitmap != null) {
            faces = detectFacesInImage(imgBitmap)
        }
        faces.forEach { faceIds.add(it.faceId) }
        val faceIdsArray = faceIds.toTypedArray()
        return microsoftIdentifyFace(personGroupId, faceIdsArray, 0.0f, 10)
    }


    suspend fun addPersonToGroup(personGroupId: String, imgUri: String): CreatePersonResult {
        return microsoftAddPersonToGroup(personGroupId, personGroupName, personGroupDescription)
    }

    suspend fun detectFacesInImage(imgBitmap: Bitmap): Array<Face> {
        val stream = ByteArrayOutputStream()
        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imageInputStream: InputStream = ByteArrayInputStream(stream.toByteArray())
        return microsoftDetectFaces(imageInputStream)
    }

    suspend fun microsoftTrainPersonGroup(personGroupId: String) =
        withContext(Dispatchers.IO) {
            faceServiceClient!!.trainLargePersonGroup(personGroupId)
            Log.d("Microsoft", "training completed")
        }

    suspend fun microsoftAddGroup(personGroupId: String, personGroupName: String, personGroupDescription: String) =
        withContext(Dispatchers.IO) {
            try {
                faceServiceClient!!.createLargePersonGroup(personGroupId, personGroupName, personGroupDescription)
            } catch (e: ClientException) {
                Log.d("Microsoft", e.error.message)
            }
        }

    suspend fun microsoftAddPersonToGroup(
        personGroupId: String,
        personName: String,
        personUserData: String
    ): CreatePersonResult =
        withContext(Dispatchers.IO) {
            TimeUnit.SECONDS.sleep(2L)
            // Start the request to creating person.
            faceServiceClient!!.createPersonInLargePersonGroup(personGroupId, personName, personUserData)
        }


    suspend fun microsoftDetectFaces(inputStream: InputStream): Array<Face> =
        withContext(Dispatchers.IO) {
            TimeUnit.SECONDS.sleep(2L)
            faceServiceClient!!.detect(
                inputStream,  /* Input stream of image to detect */
                true,       /* Whether to return face ID */
                false,       /* Whether to return face landmarks */
                /* Which face attributes to analyze, currently we support:
                   age,gender,headPose,smile,facialHair */
                null
            )
        }

    suspend fun microsoftAddFaceToPerson(
        personGroupId: String,
        personId: UUID,
        imageInputStream: InputStream,
        userData: String,
        faceRect: FaceRectangle
    ): AddPersistedFaceResult =
        withContext(Dispatchers.IO) {
            TimeUnit.SECONDS.sleep(2L)
            faceServiceClient!!.addPersonFaceInLargePersonGroup(
                personGroupId,
                personId,
                imageInputStream,
                userData,
                faceRect
            )
        }

    suspend fun microsoftGetPersons(personGroupId: String): Array<Person> =
        withContext(Dispatchers.IO) {
            faceServiceClient!!.listPersons(personGroupId)
        }

    suspend fun microsoftDeletePersonGroup(personGroupId: String) =
        withContext(Dispatchers.IO) {
            try {
                faceServiceClient!!.deleteLargePersonGroup(personGroupId)
            } catch (e: ClientException) {
                Log.d("Microsoft: ", e.error.message)
            }
        }

    suspend fun microsoftIdentifyFace(
        personGroupId: String,
        faceIds: Array<UUID>,
        confidenceThreshold: Float,
        maxNumOfCandidatesReturned: Int
    ): Array<IdentifyResult> =
        withContext(Dispatchers.IO) {
            faceServiceClient!!.identityInLargePersonGroup(
                personGroupId,   /* personGroupId */
                faceIds,                  /* faceIds */
                confidenceThreshold,
                maxNumOfCandidatesReturned
            )
        }


}