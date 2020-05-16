package de.develappers.facerecognition

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import de.develappers.facerecognition.database.FRdb
import de.develappers.facerecognition.database.dao.VisitorDao
import de.develappers.facerecognition.database.model.LogEntry
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.Visitor
import de.develappers.facerecognition.serviceAI.MicrosoftServiceAI
import de.develappers.facerecognition.utils.VISITORS_GROUP_ID
import de.develappers.facerecognition.utils.VISITOR_EXTRA
import de.develappers.facerecognition.utils.VISITOR_FIRST_TIME
import kotlinx.android.synthetic.main.activity_greeting.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GreetingActivity : AppCompatActivity() {

    private lateinit var microsoftServiceAI: MicrosoftServiceAI
    private lateinit var visitorDao: VisitorDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting)
        microsoftServiceAI = MicrosoftServiceAI(this)
        val match = intent.getSerializableExtra(VISITOR_EXTRA) as Visitor
        val firstTimeVisitor = intent.getBooleanExtra(VISITOR_FIRST_TIME, false)
        tvGreeting.text = getString(R.string.greeting, match.lastName)

        lifecycleScope.launch {
            visitorDao = FRdb.getDatabase(application, this).visitorDao()
            if (firstTimeVisitor) {
                //also send visitor data to AI services to register
                val microsoftId = microsoftServiceAI.addNewVisitorToDatabase(VISITORS_GROUP_ID, match.imgPaths.last())
                match.microsoftId = microsoftId
                visitorDao.updateVisitor(match)
            }
            //train the database with the new image
            microsoftServiceAI.microsoftTrainPersonGroup(VISITORS_GROUP_ID)
        }


        var log = LogEntry()
        log.timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        log.microsoft = match.lastName
        //TODO: if any data on recognition results in extra, then set log.amazon, log.kairos and so on to this data
        //TODO: add log to the database


    }
}
