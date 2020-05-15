package de.develappers.facerecognition

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.microsoft.projectoxford.face.contract.Candidate
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
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random.Default.nextBoolean

class MainActivity : CameraActivity() {

    private lateinit var fromCameraPath: String
    private lateinit var visitorViewModel: VisitorViewModel
    private lateinit var visitor: Visitor
    private lateinit var microsoftServiceAI: MicrosoftServiceAI
    private lateinit var visitorDao: VisitorDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        microsoftServiceAI = MicrosoftServiceAI(this)
        textureView = findViewById(R.id.textureView)

        lifecycleScope.launch {
            visitorDao = FRdb.getDatabase(application, this).visitorDao()
            //fake call to database to trigger population onCreate
            val visitor = visitorDao.findByName("efe", "gfk")
        }

        visitorViewModel = ViewModelProvider(this).get(VisitorViewModel::class.java)

        btnNo.setOnClickListener {
            navigateToRegistration()
        }

        btnYes.setOnClickListener {
            when (APP_MODE){
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

    fun findIdentifiedVisitors(results: Array<IdentifyResult>){
        // if a confident match is found, find the corresponding visitor in the local db and then go to Greeting
        lifecycleScope.launch {
        if (results[0].candidates[0].confidence > CONFIDENCE_MATCH){
                val match = visitorDao.findByMicrosoftId(results[0].candidates[0].personId.toString())
                navigateToGreeting(match)
        } else { // otherwise show all candidates in visitor list
            val serviceCandidates = mutableListOf<Candidate>()
            results.forEach {result->
                result.candidates.forEach{
                    serviceCandidates.add(it)
                }
            }
            val recognisedCandidates = createSummaryCandidateList(serviceCandidates)
            navigateToVisitorList(recognisedCandidates)
        }
        }

    }

    suspend fun createSummaryCandidateList(serviceCandidates: MutableList<Candidate>): MutableList<RecognisedCandidate>{
        val recognisedCandidates = mutableListOf<RecognisedCandidate>()
        withContext(Dispatchers.IO) {
            serviceCandidates.forEach{serviceCandidate ->
                //find the corresponding candidate in local database
                val recognisedVisitor = visitorDao.findByMicrosoftId(serviceCandidate.personId.toString())
                //check if we've added him to the list already
                val candidateFromList = recognisedCandidates.find { it.visitor == recognisedVisitor }
                if (candidateFromList!=null){
                    //if already in the list, modify the confidence level of certain service
                    candidateFromList.microsoft_conf = serviceCandidate.confidence
                } else {
                    //if not in the list, add to the recognised candidates
                    var newCandidate = RecognisedCandidate().apply {
                        this.visitor = recognisedVisitor
                        this.microsoft_conf = serviceCandidate.confidence
                    }
                    recognisedCandidates.add(newCandidate)
                }
            }
        }
        return recognisedCandidates
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


    fun getRandomVisitor(): Visitor {
        visitor = visitorViewModel.allVisitors.value!!.random()
        return visitor
    }

    private fun chooseTrialPerson(){
        var newImageUri = ""
        var databaseFolder = "database"
        //randomly decide between testing a person from the prepopulated database or a person from testbase
            if (false){
                //only familiar
                    val visitor = getRandomVisitor()
                    newImageUri = getAssetsPhotoUri(visitor.lastName!!, databaseFolder)

            } else {
                //unfamiliar faces
                    databaseFolder = "testbase"
                    newImageUri = getAssetsPhotoUri(assets.list(databaseFolder)!!.random().toString(), databaseFolder)

            }

            Log.d("Random visitor", newImageUri)
            sendPhotoToMicrosoftServicesAndEvaluate(newImageUri)
        //TODO: place for other coroutine AI services?
    }

    private fun sendPhotoToMicrosoftServicesAndEvaluate(newImageUri: String){
        lifecycleScope.launch{
            val results = microsoftServiceAI.identifyVisitor("1", newImageUri)
            findIdentifiedVisitors(results)
        }
    }

    private fun getAssetsPhotoUri(lastName: String, databaseFolder: String): String {
        val file: File = ImageHelper.saveVisitorPhotoLocally(this, lastName, galleryFolder, databaseFolder)
        return file.path
    }


}