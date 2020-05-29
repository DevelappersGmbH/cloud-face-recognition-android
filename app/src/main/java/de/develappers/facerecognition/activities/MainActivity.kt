package de.develappers.facerecognition.activities

import android.content.Intent
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import de.develappers.facerecognition.*
import de.develappers.facerecognition.database.FRdb
import de.develappers.facerecognition.database.dao.VisitorDao
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.ServiceResult
import de.develappers.facerecognition.database.model.entities.Visitor
import de.develappers.facerecognition.serviceAI.*
import de.develappers.facerecognition.serviceAI.FaceServiceAI
import de.develappers.facerecognition.utils.ImageHelper
import de.develappers.facerecognition.serviceAI.ServiceFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable
import kotlin.system.measureTimeMillis

class MainActivity : CameraActivity() {

    private lateinit var fromCameraPath: String
    @PublishedApi
    internal lateinit var visitorDao: VisitorDao
    private lateinit var ivNewVisitor: ImageView
    private lateinit var serviceProviders: MutableList<RecognitionService>
    private val possibleVisitors = mutableListOf<RecognisedCandidate>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView = findViewById(R.id.textureView)
        ivNewVisitor = findViewById(R.id.ivNewVisitor)

        //AI services
        serviceProviders = ServiceFactory.createAIServices(this,
            FaceApp.values
        )


        lifecycleScope.launch {
            visitorDao = FRdb.getDatabase(application, this).visitorDao()
            //fake call to database to trigger population on first time launch
            val visitor = visitorDao.findByName("efe", "gfk")
        }

        btnNo.setOnClickListener {
            setProgressBar()
            navigateToRegistration()
        }

        btnYes.setOnClickListener {
            setProgressBar()
            when (APP_MODE) {
                APP_MODE_REALTIME -> {
                    //take a picture and send to AI services in capture callback
                    fromCameraPath = createImageFile()
                    lockFocus()
                }
                else -> {
                    //test the application with a prepopulated database
                    chooseTrialPerson()
                }
            }
        }

        finalCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                unlockFocus()
                lifecycleScope.launch {
                    sendPhotoToServicesAndEvaluate(fromCameraPath)
                    mergeCandidates()
                }
            }
        }


    }

    private suspend fun getRandomVisitor(): Visitor {
        return visitorDao.getRandomVisitor()
    }

    private fun getAssetsPhotoUri(lastName: String, databaseFolder: String): String {
        val file: File = ImageHelper.saveVisitorPhotoLocally(this, lastName, galleryFolder, databaseFolder)
        return file.path
    }

    private fun chooseTrialPerson() {
        var newImageUri = ""
        var resFolder = "database"
        //randomly decide between testing a person from the prepopulated database or a person from testbase
        if (false) {
            //only familiar
            runBlocking {
                val randomVisitor = getRandomVisitor()
                newImageUri = getAssetsPhotoUri(randomVisitor.lastName!!, resFolder)
            }
        } else {
            //unfamiliar faces
            resFolder = "testbase"
            newImageUri = getAssetsPhotoUri(assets.list(resFolder)!!.random().toString(), resFolder)

        }
        Log.d("Random visitor", newImageUri)

        // after a test person has been chosen, send the photo for recognition
        setNewVisitorToPreview(newImageUri)
        lifecycleScope.launch {
            sendPhotoToServicesAndEvaluate(newImageUri)
            mergeCandidates()
            //findSingleMatchOrSuggestList()
        }
    }

    private fun setNewVisitorToPreview(newImageUri: String) {
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(Uri.parse(newImageUri), this)
        ivNewVisitor.setImageBitmap(imgBitmap)
    }

    suspend fun sendPhotoToServicesAndEvaluate(newImageUri: String) = withContext(Dispatchers.IO) {
        serviceProviders.forEach {
            if (it.isActive) {
                launch {
                    findIdentifiedVisitors(newImageUri, it)
                }
            }
        }
    }

    suspend fun findIdentifiedVisitors(newImageUri: String, service: RecognitionService) {
        var candidates = listOf<Any>()
        // measure time needed for the service to identify a person
        val identificationResponseTime = measureTimeMillis {
            candidates = service.identifyVisitor(VISITORS_GROUP_ID, newImageUri)
        }
        // merge possible visitors from all services
        candidates.forEach { candidate ->
            val localIdPath = service.defineLocalIdPath(candidate)
            val newCandidate = RecognisedCandidate().apply {
                this.visitor = findVisitor(localIdPath, service)
                this.serviceResults.add(ServiceResult(
                    service.provider,
                    identificationResponseTime,
                    service.defineConfidenceLevel(candidate)
                ))
            }
            possibleVisitors.add(newCandidate)
        }
    }

    fun mergeCandidates() {
        val mergedVisitors = mutableListOf<RecognisedCandidate>()
        possibleVisitors.forEach { candidate ->
            if (mergedVisitors.find { candidate.visitor == it.visitor } == null){
                val mergedCandidate = RecognisedCandidate().apply { this.visitor = candidate.visitor }
                possibleVisitors.filter { candidate.visitor == it.visitor }.forEach {
                    mergedCandidate.serviceResults.add(it.serviceResults.last())
                }
                mergedVisitors.add(mergedCandidate)
            }
        }
        // filter all candidates where all services returned high confidence
        val sureMatches = mergedVisitors.filter { candidate ->
            (candidate.serviceResults.all { it.confidence > CONFIDENCE_MATCH })
        }
        // if only one sure match is found by all services, greet him
        if (sureMatches.count() == 1) {
            //return the only sure match
            navigateToGreeting(sureMatches[0])
            return
        }
        //otherwise show a list of options
        navigateToVisitorList(mergedVisitors)
    }


    suspend fun findVisitor(localIdPath: String, service: RecognitionService): Visitor {
        return when (service) {
            is AmazonServiceAI -> visitorDao.findByAmazonFaceId(localIdPath)
            is MicrosoftServiceAI -> visitorDao.findByMicrosoftId(localIdPath)
            is FaceServiceAI -> visitorDao.findByFaceId(localIdPath)
            else -> Visitor(null, null, null)
        }
    }


    private fun setProgressBar() {
        progressBar.visibility = VISIBLE
        btnYes.isClickable = false
        btnNo.isClickable = false

    }

    private fun navigateToRegistration() {
        intent = Intent(this@MainActivity, RegistrationActivity::class.java)
        startActivity(intent)
    }


    @PublishedApi
    internal fun navigateToGreeting(match: RecognisedCandidate) {
        intent = Intent(this@MainActivity, GreetingActivity::class.java)
        intent.putExtra(RECOGNISED_CANDIDATE_EXTRA, match)
        startActivity(intent)
    }

    @PublishedApi
    internal fun navigateToVisitorList(possibleVisitors: List<RecognisedCandidate>) {
        intent = Intent(this@MainActivity, VisitorListActivity::class.java)
        intent.putExtra(CANDIDATES_EXTRA, possibleVisitors as Serializable)
        startActivity(intent)
    }

}