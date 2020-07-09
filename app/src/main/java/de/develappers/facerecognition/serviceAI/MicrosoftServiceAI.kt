package de.develappers.facerecognition.serviceAI

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.microsoft.projectoxford.face.contract.*
import com.microsoft.projectoxford.face.rest.ClientException
import de.develappers.facerecognition.*
import de.develappers.facerecognition.database.model.entities.Visitor
import de.develappers.facerecognition.utils.ImageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

class MicrosoftServiceAI(
    val context: Context,
    override var provider: String,
    override var isActive: Boolean) : RecognitionService {

    // Get an instance of face service client.
    val faceServiceClient = FaceApp.microsoftServiceClient
    val personGroupId = VISITORS_GROUP_ID
    val personGroupName = VISITORS_GROUP_NAME
    val personGroupDescription = VISITORS_GROUP_DESCRIPTION
    val userData = "user data"

    override suspend fun train() {
        microsoftTrainPersonGroup(personGroupId)
    }

    override suspend fun deletePersonGroup(personGroupId: String) {
        microsoftDeletePersonGroup(personGroupId)
    }

    //step 1
    override suspend fun addPersonGroup(personGroupId: String) {
        microsoftAddGroup(personGroupId, personGroupName, personGroupDescription)
    }

    override suspend fun addNewVisitorToDatabase(personGroupId: String, imgUri: String, visitor: Visitor) {
        //step 2
        val createPersonResult = addPersonToGroup(personGroupId)
        setServiceId(visitor, createPersonResult.personId.toString())
        addNewImage(personGroupId, imgUri, visitor)
    }

    override suspend fun addNewImage(personGroupId: String, imgUri: String, visitor: Visitor) {
        Log.d("Retrieving", imgUri)
        var imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        var faces = arrayOf<Face>()
        imgBitmap?.let { //step 3
            faces = detectFacesInImage(it) }
        faces.forEach {
            val stream = ByteArrayOutputStream()
            imgBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val imageInputStream: InputStream = ByteArrayInputStream(stream.toByteArray())
            //step 4 (given there is only one person/face in the captured photo and we can add all detected faces to this person)
            microsoftAddFaceToPerson(
                personGroupId,
                UUID.fromString(visitor.microsoftId),
                imageInputStream,
                userData,
                it.faceRectangle
            )
        }
    }

    override suspend fun identifyVisitor(personGroupId: String, imgUri: String): List<Any> {
        val faceIds: MutableList<UUID> = mutableListOf()
        val imageUri = Uri.parse(imgUri)
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(imageUri, context)
        var faces = arrayOf<Face>()
        if (imgBitmap != null) {
            faces = detectFacesInImage(imgBitmap)
        }
        faces.forEach { faceIds.add(it.faceId) }
        val faceIdsArray = faceIds.toTypedArray()
        val candidates = mutableListOf<Candidate>()
            if (faceIdsArray.isNotEmpty()){
                val identifyResults = microsoftIdentifyFace(personGroupId, faceIdsArray, CONFIDENCE_CANDIDATE.toFloat(), RETURN_RESULT_COUNT)
                candidates.apply {
                    identifyResults.forEach {
                        this.addAll(it.candidates)
                    }
            }
        }
        return candidates
    }


    suspend fun addPersonToGroup(personGroupId: String): CreatePersonResult {
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
                faceServiceClient?.createLargePersonGroup(personGroupId, personGroupName, personGroupDescription)
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

    override fun setServiceId(visitor: Visitor, id: String){
        visitor.microsoftId = id
    }


    override fun defineLocalIdPath(candidate: Any): String {
        candidate as Candidate
        return candidate.personId.toString()
    }

    override fun defineConfidenceLevel(candidate: Any): Double {
        candidate as Candidate
        return candidate.confidence
    }

}