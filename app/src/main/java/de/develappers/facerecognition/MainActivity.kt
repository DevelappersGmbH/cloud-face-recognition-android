package de.develappers.facerecognition

import android.content.Intent
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import de.develappers.facerecognition.database.FRdb
import de.develappers.facerecognition.database.VisitorViewModel
import de.develappers.facerecognition.database.dao.VisitorDao
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.Visitor
import de.develappers.facerecognition.serviceAI.AmazonServiceAI
import de.develappers.facerecognition.serviceAI.ImageHelper
import de.develappers.facerecognition.serviceAI.MicrosoftServiceAI
import de.develappers.facerecognition.serviceAI.RecognitionService
import de.develappers.facerecognition.utils.*
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
    private lateinit var visitorViewModel: VisitorViewModel
    @PublishedApi
    internal lateinit var visitorDao: VisitorDao
    private lateinit var ivNewVisitor: ImageView
    private val serviceProviders = mutableListOf<RecognitionService>()
    private val possibleVisitors = mutableListOf<RecognisedCandidate>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView = findViewById(R.id.textureView)
        ivNewVisitor = findViewById(R.id.ivNewVisitor)

        //AI services
        val microsoftServiceAI = MicrosoftServiceAI(this)
        val amazonServiceAI = AmazonServiceAI(this)

        serviceProviders.add(microsoftServiceAI)
        serviceProviders.add(amazonServiceAI)


        lifecycleScope.launch {
            visitorDao = FRdb.getDatabase(application, this).visitorDao()
            //fake call to database to trigger population on first time launch
            //val visitor = visitorDao.findByName("efe", "gfk")
        }
        visitorViewModel = ViewModelProvider(this).get(VisitorViewModel::class.java)
        // debugging the database population
        visitorViewModel.allVisitors.observe(this, Observer { visitors ->
            if (visitors.isNotEmpty()) {
                Log.d("New visitor in db : ", visitors.last().lastName!!)
            }
        })

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
                    findSingleMatchOrSuggestList()
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
            findSingleMatchOrSuggestList()
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
        // measure time needed for the service to indentify a person
        val identificationTime = measureTimeMillis {
            candidates = service.identifyVisitor(VISITORS_GROUP_ID, newImageUri)
        }
        // merge possible visitors from all services
        candidates.forEach { candidate ->
            val localIdPath = service.defineLocalIdPath(candidate)
            val recognisedVisitor = findVisitor(localIdPath, service)
            if (possibleVisitors.find { it.visitor == recognisedVisitor } == null) {
                val newCandidate = RecognisedCandidate().apply {
                    this.visitor = recognisedVisitor
                    service.setConfidenceLevel(candidate, this)
                }
                possibleVisitors.add(newCandidate)
            }
        }
    }

    fun findSingleMatchOrSuggestList() {
        // if a sure match is found by all services, greet him
        possibleVisitors.forEach { candidate ->
            if (((candidate.microsoft_conf > CONFIDENCE_MATCH) || !MICROSOFT) &&
                ((candidate.amazon_conf > CONFIDENCE_MATCH) || !AMAZON) &&
                ((candidate.face_conf > CONFIDENCE_MATCH) || !FACE) &&
                ((candidate.kairos_conf > CONFIDENCE_MATCH) || !KAIROS) &&
                ((candidate.luxand_conf > CONFIDENCE_MATCH) || !LUXAND)) {
                //return the match
                navigateToGreeting(candidate)
                return
            }
        }
        navigateToVisitorList(possibleVisitors)
    }


    suspend fun findVisitor(localIdPath: String, service: RecognitionService): Visitor {
        return when (service) {
            is AmazonServiceAI -> visitorDao.findByAmazonFaceId(localIdPath)
            is MicrosoftServiceAI -> visitorDao.findByMicrosoftId(localIdPath)
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