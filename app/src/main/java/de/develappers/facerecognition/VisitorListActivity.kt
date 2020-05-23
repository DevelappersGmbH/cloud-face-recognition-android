package de.develappers.facerecognition

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import de.develappers.facerecognition.adapter.VisitorListAdapter
import de.develappers.facerecognition.database.FRdb
import de.develappers.facerecognition.database.VisitorViewModel
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.listeners.OnVisitorItemClickedListener
import de.develappers.facerecognition.database.model.Visitor
import de.develappers.facerecognition.utils.CANDIDATES_EXTRA
import de.develappers.facerecognition.utils.RECOGNISED_CANDIDATE_EXTRA
import de.develappers.facerecognition.utils.VISITOR_EXTRA
import kotlinx.android.synthetic.main.activity_visitor_list.*
import kotlinx.android.synthetic.main.item_visitor.view.*

class VisitorListActivity : AppCompatActivity(), OnVisitorItemClickedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visitor_list)
        showAlertDialog()

        val titles = listOf (getString(R.string.microsoft),
            getString(R.string.amazon),
            getString(R.string.face),
            getString(R.string.kairos),
            getString(R.string.luxand))

        val recognisedCandidates = intent.extras?.get(CANDIDATES_EXTRA) as List<RecognisedCandidate>

        titles.forEach{title ->
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
            textView.layoutParams = params
            responseView.layoutParams = params

            headerItem.probabilityView.addView(textView)
            responseTimesItem.probabilityView.addView(responseView)
            responseTimesItem.fullNameView.text = getString(R.string.response_time)
        }

        var adapter = VisitorListAdapter(this, this, titles)
        rvVisitorList.adapter = adapter
        rvVisitorList.layoutManager = LinearLayoutManager(this)

        adapter.setVisitors(recognisedCandidates)


        btnNoMatch.setOnClickListener {
            intent = Intent(this@VisitorListActivity, RegistrationActivity::class.java)
            startActivity(intent)
        }

    }


    override fun onVisitorItemClicked(candidate: RecognisedCandidate){
        intent = Intent(this@VisitorListActivity, GreetingActivity::class.java)
        //TODO: if the visitor is not new, but from here, add new photo to his entity in database in the next actiivty
        intent.putExtra(RECOGNISED_CANDIDATE_EXTRA, candidate);
        startActivity(intent)
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
}
