package de.develappers.facerecognition.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import de.develappers.facerecognition.*
import de.develappers.facerecognition.database.FRdb
import de.develappers.facerecognition.database.dao.VisitorDao
import de.develappers.facerecognition.database.model.entities.LogEntry
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.entities.Visitor
import de.develappers.facerecognition.serviceAI.RecognitionService
import de.develappers.facerecognition.serviceAI.ServiceFactory
import kotlinx.android.synthetic.main.activity_greeting.*
import kotlinx.coroutines.*
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class GreetingActivity : AppCompatActivity() {

    private lateinit var visitorDao: VisitorDao
    private var log = LogEntry()
    private lateinit var serviceProviders: MutableList<RecognitionService>
    lateinit var aiJob: Job
    lateinit var dbJob: Job


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting)

        val imgPath: String? = intent.extras?.getString(NEW_IMAGE_PATH_EXTRA)

        //AI services
        serviceProviders = ServiceFactory.createAIServices(
            this,
            FaceApp.values
        )

        lifecycleScope.launch {
            visitorDao = FRdb.getDatabase(application, this).visitorDao()

            if (intent.hasExtra(VISITOR_EXTRA)) {
                val visitor = intent.getSerializableExtra(VISITOR_EXTRA) as Visitor
                tvGreeting.text = getString(R.string.greeting, visitor.lastName)

                //register the visitor within AI services and set visitor service ID in the database
                dbJob = lifecycleScope.launch {
                    //save new visitor in database, get visitor id
                    visitor.visitorId = visitorDao.insert(visitor)
                    registerVisitorInAIServices(visitor)
                    visitorDao.updateVisitor(visitor)
                    registerNewVisitor(visitor.visitorId.toString())
                }

                //train the database with the new image, if needed
                aiJob = lifecycleScope.launch {
                    trainServices()
                }

            }

            if (intent.hasExtra(RECOGNISED_CANDIDATE_EXTRA)) {
                val recognisedCandidate = intent.getSerializableExtra(RECOGNISED_CANDIDATE_EXTRA) as RecognisedCandidate
                tvGreeting.text = getString(R.string.greeting, recognisedCandidate.visitor.lastName)

                aiJob = lifecycleScope.launch {
                    addNewPhotoToAIServices(imgPath, recognisedCandidate.visitor)
                    trainServices()
                }
                dbJob = lifecycleScope.launch {
                    addNewPhotoPathToDatabase(imgPath, recognisedCandidate.visitor)

                }
                registerRepeatingVisitor(recognisedCandidate)
            }

            aiJob.join()
            dbJob.join()
            navigateToStart()

        }

    }

    private fun navigateToStart() {
        intent = Intent(this@GreetingActivity, MainActivity::class.java)
        startActivity(intent)
    }

    private fun registerNewVisitor(visitorId: String) {
        log.timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        log.trueVisitorId = visitorId
        writeToLogbook()

    }

    private fun registerRepeatingVisitor(candidate: RecognisedCandidate) {
        log.timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        log.serviceResults = candidate.serviceResults
        log.trueVisitorId = candidate.visitor.visitorId.toString()
        writeToLogbook()
    }

    private fun writeToLogbook() {
        println("Logged visit: ${Gson().toJson(log)}")
        File(FaceApp.storageDirectory, "logbook.txt").appendText("${Gson().toJson(log)}\n")
    }

    private suspend fun addNewPhotoToAIServices(imgPath: String?, visitor: Visitor) = withContext(Dispatchers.IO) {
        // withContext waits for all children coroutines
        serviceProviders.forEach { service ->
            if (service.isActive) {
                launch {
                    imgPath?.let { service.addNewImage(VISITORS_GROUP_ID, it, visitor) }
                }
            }
        }
    }

    private suspend fun trainServices() = withContext(Dispatchers.IO) {
        // withContext waits for all children coroutines
        serviceProviders.forEach { service ->
            if (service.isActive) {
                launch {
                    service.train()
                }
            }
        }
    }

    private suspend fun addNewPhotoPathToDatabase(imgPath: String?, visitor: Visitor) {
        imgPath?.let { visitor.imgPaths.add(it) }
        visitorDao.updateVisitor(visitor)
    }

    private suspend fun registerVisitorInAIServices(visitor: Visitor) = withContext(Dispatchers.IO) {
        // withContext waits for all children coroutines
        serviceProviders.forEach {
            if (it.isActive) {
                launch {
                    it.addNewVisitorToDatabase(VISITORS_GROUP_ID, visitor.imgPaths.last(), visitor)
                }
            }
        }
    }

}
