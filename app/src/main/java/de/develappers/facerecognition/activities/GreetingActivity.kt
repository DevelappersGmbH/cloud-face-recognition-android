package de.develappers.facerecognition.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
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
import java.text.SimpleDateFormat
import java.util.*

class GreetingActivity : AppCompatActivity() {

    private lateinit var visitorDao: VisitorDao
    private var log = LogEntry()
    private lateinit var serviceProviders:MutableList<RecognitionService>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting)

        //AI services
        serviceProviders = ServiceFactory.createAIServices(this,
            FaceApp.values
        )

        if (intent.hasExtra(VISITOR_EXTRA)) {
            val visitor = intent.getSerializableExtra(VISITOR_EXTRA) as Visitor
            tvGreeting.text = getString(R.string.greeting, visitor.lastName)

            lifecycleScope.launch {
                visitorDao = FRdb.getDatabase(application, this).visitorDao()
                //register the visitor within AI services and sets visitor service ID in the datasbe
                registerVisitorInAIServices(visitor)
                //save new visitor in database, get visitor id
                val newVisitorId = visitorDao.insert(visitor)
                registerNewVisitor(newVisitorId.toString())

                //train the database with the new image, if needed
                serviceProviders.forEach {
                    it.train()
                }
            }
        }

        if (intent.hasExtra(RECOGNISED_CANDIDATE_EXTRA)) {
            val recognisedCandidate = intent.getSerializableExtra(RECOGNISED_CANDIDATE_EXTRA) as RecognisedCandidate
            tvGreeting.text = getString(R.string.greeting, recognisedCandidate.visitor.lastName)
            registerRepeatingVisitor(recognisedCandidate)
        }


    }

    fun registerNewVisitor(visitorId: String) {
        log.timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        log.trueVisitorId = visitorId

    }

    fun registerRepeatingVisitor(candidate: RecognisedCandidate) {
        log.timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        log.serviceResults = candidate.serviceResults
        log.trueVisitorId = candidate.visitor.visitorId.toString()

    }

    suspend fun registerVisitorInAIServices(visitor: Visitor) = withContext(Dispatchers.IO) {
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
