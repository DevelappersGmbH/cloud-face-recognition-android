package de.develappers.facerecognition.activities

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import de.develappers.facerecognition.*
import de.develappers.facerecognition.TTS.TTS
import de.develappers.facerecognition.adapter.VisitorListAdapter
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.listeners.OnVisitorItemClickedListener
import kotlinx.android.synthetic.main.activity_visitor_list.*
import kotlinx.android.synthetic.main.item_visitor.view.*
import java.io.Serializable

class VisitorListActivity : AppCompatActivity(), OnVisitorItemClickedListener {

    var imgPath: String? = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visitor_list)

        speak(getString(R.string.not_recognised))
        showAlertDialog()


        val recognisedCandidates = intent.extras?.get(CANDIDATES_EXTRA) as List<RecognisedCandidate>
        imgPath = intent.extras?.getString(NEW_IMAGE_PATH_EXTRA)

        FaceApp.values.keys.forEach{ key ->
            getString(key).also {title ->
                val textView = TextView(this)
                val responseView = TextView(this)
                val responseTime = recognisedCandidates.flatMap { it.serviceResults }.firstOrNull { it.provider == title }?.identificationTime
                textView.text = title
                responseView.text = responseTime.toString()
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.0f
                )
                params.gravity = Gravity.CENTER;
                textView.layoutParams = params
                responseView.layoutParams = params
                textView.gravity = Gravity.CENTER;
                responseView.gravity = Gravity.CENTER;

                headerItem.probabilityView.addView(textView)
                responseTimesItem.probabilityView.addView(responseView)
                responseTimesItem.fullNameView.text = getString(R.string.response_time)  }

        }

        var adapter = VisitorListAdapter(this, this)
        rvVisitorList.adapter = adapter
        rvVisitorList.layoutManager = LinearLayoutManager(this)

        adapter.setVisitors(recognisedCandidates)


        btnNoMatch.setOnClickListener {
            navigateToRegistartion()
        }

    }


    override fun onVisitorItemClicked(candidate: RecognisedCandidate){
        navigateToGreeting(candidate)
    }

    private fun showAlertDialog(){
        // build alert dialog
        val dialogBuilder = AlertDialog.Builder(this)

        // set message of alert dialog
        dialogBuilder.setMessage(R.string.not_recognised)
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton(R.string.ok, DialogInterface.OnClickListener {
                    dialog, id -> dialog.cancel()
            })

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle(R.string.oops)
        // show alert dialog
        alert.show()
    }

    fun speak(text: String){
        TTS().initialize(applicationContext, text)
    }

    fun navigateToRegistartion(){
        intent = Intent(this@VisitorListActivity, RegistrationActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun navigateToGreeting(candidate: RecognisedCandidate){
        intent = Intent(this@VisitorListActivity, GreetingActivity::class.java)
        //if the visitor is not new, but was not recognised, add new photo to his entity in database in the next actiivty
        intent.putExtra(RECOGNISED_CANDIDATE_EXTRA, candidate as Serializable);
        intent.putExtra(NEW_IMAGE_PATH_EXTRA, imgPath)
        startActivity(intent)
    }
}
