package de.develappers.facerecognition

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import de.develappers.facerecognition.database.model.LogEntry
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.Visitor
import de.develappers.facerecognition.utils.VISITOR_EXTRA
import kotlinx.android.synthetic.main.activity_greeting.*
import java.text.SimpleDateFormat
import java.util.*

class GreetingActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_greeting)
        val match = intent.getSerializableExtra(VISITOR_EXTRA) as Visitor
        tvGreeting.text = getString(R.string.greeting, match.lastName)

        var log = LogEntry()
        log.timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        log.microsoft = match.lastName
        //TODO: if any data on recognition results in extra, then set log.amazon, log.kairos and so on to this data
        //TODO: add log to the database
        // find corresponding visiotors in local databse to the one recognised by AI services


    }
}
