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

class MainActivity : CameraActivity() {

    private lateinit var fromCameraPath: String
    private lateinit var visitorViewModel: VisitorViewModel
    private lateinit var microsoftServiceAI: MicrosoftServiceAI
    private lateinit var amazonServiceAI: AmazonServiceAI
    @PublishedApi
    internal lateinit var visitorDao: VisitorDao
    private lateinit var ivNewVisitor: ImageView
    private var serviceProviders = mutableListOf<RecognitionService>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView = findViewById(R.id.textureView)
        ivNewVisitor = findViewById(R.id.ivNewVisitor)

        //AI services
        microsoftServiceAI = MicrosoftServiceAI(this)
        amazonServiceAI = AmazonServiceAI(this)

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
                }
            }
        }


    }

    suspend fun getRandomVisitor(): Visitor {
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

        setNewVisitorToPreview(newImageUri)
        lifecycleScope.launch {
            sendPhotoToServicesAndEvaluate(newImageUri)
        }
    }

    private fun setNewVisitorToPreview(newImageUri: String) {
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(Uri.parse(newImageUri), this)
        ivNewVisitor.setImageBitmap(imgBitmap)
    }

    suspend fun sendPhotoToServicesAndEvaluate(newImageUri: String) = withContext(Dispatchers.IO){

        serviceProviders.forEach{
            if (it.isActive){
                launch {
                    findIdentifiedVisitors(newImageUri, it)
                }
            }
        }
        /*//amazon
        lifecycleScope.launch {
            val results = amazonServiceAI.identifyVisitor(VISITORS_GROUP_ID, newImageUri)
            //a list of face matches, each containing similarity, face id and external id (img path)
            findIdentifiedVisitors(results)
        }*/
        //kairos
        //face
        //luxand
    }
     fun findIdentifiedVisitors (newImageUri: String, service: RecognitionService) {
         // if a sure match is found, greet him
        lifecycleScope.launch {
            val candidates = service.identifyVisitor(VISITORS_GROUP_ID, newImageUri)
            candidates.forEach { candidate ->
                if (service.defineConfidenceLevel(candidate) > CONFIDENCE_MATCH) {
                    val match = findVisitor(candidate, service)
                    //return the match
                    navigateToGreeting(match)
                    return@launch
                }
            }

            // otherwise show all candidates in visitor list
            val possibleVisitors = mutableListOf<RecognisedCandidate>()
            candidates.forEach { candidate ->
                val recognisedVisitor = findVisitor(candidate, service)
                if (possibleVisitors.find { it.visitor == recognisedVisitor } == null) {
                    val newCandidate = RecognisedCandidate().apply {
                        this.visitor = recognisedVisitor
                        service.setConfidenceLevel(candidate, this)
                    }
                    possibleVisitors.add(newCandidate)
                }
            }
            navigateToVisitorList(possibleVisitors)
        }
    }

    fun findSingleMatch (newImageUri: String, service: RecognitionService) {

    }


    suspend fun findVisitor(candidate: Any, service: RecognitionService): Visitor {
        val localIdPath = service.defineLocalIdPath(candidate)
        return when (service){
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
    internal fun navigateToGreeting(match: Visitor) {
        intent = Intent(this@MainActivity, GreetingActivity::class.java)
        intent.putExtra(VISITOR_EXTRA, match)
        startActivity(intent)
    }

    @PublishedApi
    internal fun navigateToVisitorList(possibleVisitors: List<RecognisedCandidate>) {
        intent = Intent(this@MainActivity, VisitorListActivity::class.java)
        intent.putExtra(CANDIDATES_EXTRA, possibleVisitors as Serializable)
        startActivity(intent)
    }

}