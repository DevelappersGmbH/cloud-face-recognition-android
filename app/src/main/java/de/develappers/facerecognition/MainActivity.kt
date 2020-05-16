package de.develappers.facerecognition

import android.content.Intent
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.microsoft.projectoxford.face.contract.IdentifyResult
import de.develappers.facerecognition.database.FRdb
import de.develappers.facerecognition.database.VisitorViewModel
import de.develappers.facerecognition.database.dao.VisitorDao
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.Visitor
import de.develappers.facerecognition.serviceAI.ImageHelper
import de.develappers.facerecognition.serviceAI.MicrosoftServiceAI
import de.develappers.facerecognition.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.progressBar
import kotlinx.android.synthetic.main.activity_registration.*
import kotlinx.coroutines.*
import java.io.File
import java.io.Serializable
import kotlin.random.Random.Default.nextBoolean

class MainActivity : CameraActivity() {

    private lateinit var fromCameraPath: String
    private lateinit var visitorViewModel: VisitorViewModel
    private lateinit var microsoftServiceAI: MicrosoftServiceAI
    private lateinit var visitorDao: VisitorDao
    private lateinit var ivNewVisitor: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView = findViewById(R.id.textureView)
        ivNewVisitor = findViewById(R.id.ivNewVisitor)


        microsoftServiceAI = MicrosoftServiceAI(this)


        lifecycleScope.launch {
            visitorDao = FRdb.getDatabase(application, this).visitorDao()
            //fake call to database to trigger population on first time launch
            val visitor = visitorDao.findByName("efe", "gfk")
        }

        visitorViewModel = ViewModelProvider(this).get(VisitorViewModel::class.java)
        // debugging the database population
        visitorViewModel.allVisitors.observe(this, Observer { visitors ->
            if (visitors.isNotEmpty()){
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
                    //TODO: take a picture and send to AI services
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
                sendPhotoToMicrosoftServicesAndEvaluate(fromCameraPath)
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
        if (true) {
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
        sendPhotoToMicrosoftServicesAndEvaluate(newImageUri)
        //TODO: place for other coroutine AI services?
    }

    private fun setNewVisitorToPreview(newImageUri: String) {
        val imgBitmap = ImageHelper.loadSizeLimitedBitmapFromUri(Uri.parse(newImageUri), this)
        ivNewVisitor.setImageBitmap(imgBitmap)
    }

    private fun sendPhotoToMicrosoftServicesAndEvaluate(newImageUri: String) {
        //wait until we get recognition result from microsoft
        lifecycleScope.launch {
            val results = microsoftServiceAI.identifyVisitor(VISITORS_GROUP_ID, newImageUri)
            findIdentifiedVisitors(results)
        }
        //amazon
        //kairos
        //face
        //luxand
    }

    fun findIdentifiedVisitors(results: Array<IdentifyResult>) {
        // if a confident match is found, find the corresponding visitor in the local db and then go to Greeting
        lifecycleScope.launch {
            if (results[0].candidates[0].confidence > CONFIDENCE_MATCH) {
                val match = visitorDao.findByMicrosoftId(results[0].candidates[0].personId.toString())
                //return the match
                navigateToGreeting(match)
            } else { // otherwise show all candidates in visitor list
                val possibleVisitors = visitorViewModel.addCandidatesToSelection(results)
                navigateToVisitorList(possibleVisitors)
            }
        }

    }

    private fun setProgressBar(){
        progressBar.visibility = VISIBLE
        btnYes.isClickable = false
        btnNo.isClickable = false

    }

    private fun navigateToRegistration() {
        intent = Intent(this@MainActivity, RegistrationActivity::class.java)
        startActivity(intent)
    }


    private fun navigateToGreeting(match: Visitor) {
        intent = Intent(this@MainActivity, GreetingActivity::class.java)
        intent.putExtra(VISITOR_EXTRA, match)
        startActivity(intent)
    }

    private fun navigateToVisitorList(possibleVisitors: List<RecognisedCandidate>) {
        intent = Intent(this@MainActivity, VisitorListActivity::class.java)
        intent.putExtra(CANDIDATES_EXTRA, possibleVisitors as Serializable)
        startActivity(intent)
    }

}