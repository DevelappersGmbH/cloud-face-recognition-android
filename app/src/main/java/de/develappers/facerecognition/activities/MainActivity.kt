package de.develappers.facerecognition.activities

import android.R.attr.data
import android.content.Intent
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.Log
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import de.develappers.facerecognition.*
import de.develappers.facerecognition.TTS.TTS
import de.develappers.facerecognition.database.FRdb
import de.develappers.facerecognition.database.dao.VisitorDao
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.ServiceResult
import de.develappers.facerecognition.database.model.entities.AnalysisData
import de.develappers.facerecognition.database.model.entities.Visitor
import de.develappers.facerecognition.serviceAI.*
import de.develappers.facerecognition.utils.ImageHelper
import de.develappers.facerecognition.serviceAI.ServiceFactory
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.LuxandFace
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable
import kotlin.random.Random.Default.nextBoolean
import kotlin.system.measureTimeMillis


class MainActivity : CameraActivity() {


    private var trueId = 777L
    private var runCount = 0
    private lateinit var fromCameraPath: String
    private lateinit var tts: TTS
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

        speak(tvFirstTime.text.toString())

        //AI services
        serviceProviders = ServiceFactory.createAIServices(
            this,
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
        var resFolder = "database"
        //randomly decide between testing a person from the prepopulated database or a person from testbase
        if (nextBoolean()) {
            //only familiar
            runBlocking {
                Toast.makeText(applicationContext, "Known person", LENGTH_LONG).show()
                val randomVisitor = getRandomVisitor()
                trueId = randomVisitor.visitorId
                fromCameraPath = getAssetsPhotoUri(randomVisitor.lastName!!, resFolder)
            }
        } else {
            //unfamiliar faces
            trueId = 777L
            Toast.makeText(applicationContext, "Unknown person", LENGTH_LONG).show()
            resFolder = "testbase"
            fromCameraPath = getAssetsPhotoUri(assets.list(resFolder)!!.random().toString(), resFolder)

        }
        Log.d("Random visitor", fromCameraPath)

        // after a test person has been chosen, send the photo for recognition
        setNewVisitorToPreview(fromCameraPath)
        lifecycleScope.launch {
            sendPhotoToServicesAndEvaluate(fromCameraPath)

            /*// after getting all the results for merged candidates, we can save them to analyse further
            //also save "known" or "unknown" person and the run count
            possibleVisitors.forEach {
                writeToLogbook(
                    AnalysisData(
                        it.visitor.visitorId,
                        it.visitor.firstName,
                        it.visitor.lastName,
                        it.serviceResults[0].provider,
                        it.serviceResults[0].confidence,
                        it.serviceResults[0].identificationTime,
                        trueId,
                        runCount
                        )
                )
            }
            //instead of merging candidates for testing purposes run chooseTrialperson again until run_count is 50
            runCount++
            if (runCount < 50) {
                chooseTrialPerson()
            }*/

            mergeCandidates()
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
        // unite possible visitors from all services

        if (service is LuxandServiceAI){
            candidates = candidates.sortedByDescending { (it as LuxandFace).probability }
        }
        candidates.forEachIndexed { index, candidate ->
            if (index < RETURN_RESULT_COUNT){
                val localIdPath = service.defineLocalIdPath(candidate)
                findVisitor(localIdPath, service)?.let { val newCandidate = RecognisedCandidate().apply {
                    this.visitor = it
                    this.serviceResults.add(
                        ServiceResult(
                            service.provider,
                            identificationResponseTime,
                            service.defineConfidenceLevel(candidate)
                        )
                    )
                }
                    possibleVisitors.add(newCandidate) }
            }
        }
    }

    fun mergeCandidates() {
        val mergedVisitors = mutableListOf<RecognisedCandidate>()
        possibleVisitors.forEach { candidate ->
            //if there is no such candidate yet, add him
            if (mergedVisitors.find { candidate.visitor == it.visitor } == null) {
                val mergedCandidate = RecognisedCandidate().apply { this.visitor = candidate.visitor }
                //filter all candidates with current visitor id and merge service results from all services
                possibleVisitors.filter { candidate.visitor == it.visitor }.forEach {
                    //if there is already result from this service for the same visitor, choose the highest rate result
                    val exisitingServiceResult =
                        mergedCandidate.serviceResults.find { result -> result.provider == it.serviceResults.last().provider }
                    if (exisitingServiceResult != null) {
                        if (exisitingServiceResult.confidence < it.serviceResults.last().confidence) {
                            mergedCandidate.serviceResults.remove(exisitingServiceResult)
                            mergedCandidate.serviceResults.add(it.serviceResults.last())
                        }
                    } else {
                        mergedCandidate.serviceResults.add(it.serviceResults.last())
                    }

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


    suspend fun findVisitor(localIdPath: String, service: RecognitionService): Visitor? {
        return when (service) {
            is AmazonServiceAI -> visitorDao.findByAmazonFaceId(localIdPath)
            is MicrosoftServiceAI -> visitorDao.findByMicrosoftId(localIdPath)
            is FaceServiceAI -> visitorDao.findByFaceId(localIdPath)
            is KairosServiceAI -> visitorDao.findByKairosId(localIdPath)
            is LuxandServiceAI -> visitorDao.findByLuxandId(localIdPath)
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
        finish()
    }


    @PublishedApi
    internal fun navigateToGreeting(match: RecognisedCandidate) {
        intent = Intent(this@MainActivity, GreetingActivity::class.java)
        intent.putExtra(RECOGNISED_CANDIDATE_EXTRA, match)
        intent.putExtra(NEW_IMAGE_PATH_EXTRA, fromCameraPath)
        startActivity(intent)
        finish()
    }

    @PublishedApi
    internal fun navigateToVisitorList(possibleVisitors: List<RecognisedCandidate>) {
        intent = Intent(this@MainActivity, VisitorListActivity::class.java)
        intent.putExtra(CANDIDATES_EXTRA, possibleVisitors as Serializable)
        intent.putExtra(NEW_IMAGE_PATH_EXTRA, fromCameraPath)
        startActivity(intent)
        finish()
    }

    fun speak(text: String){
        TTS().initialize(applicationContext, text)
    }

    private fun writeToLogbook(data: AnalysisData) {
        File(FaceApp.storageDirectory, "AnalysisData.txt").appendText("${Gson().toJson(data)}\n")
    }

}