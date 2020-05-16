package de.develappers.facerecognition

import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import de.develappers.facerecognition.utils.VISITOR_EXTRA
import kotlinx.android.synthetic.main.activity_visitor_list.*

class VisitorListActivity : AppCompatActivity(), OnVisitorItemClickedListener {

    private lateinit var visitorViewModel: VisitorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visitor_list)
        showAlertDialog()

        var adapter = VisitorListAdapter(this, this)
        rvVisitorList.adapter = adapter
        rvVisitorList.layoutManager = LinearLayoutManager(this)
        
        val recognisedCandidates = intent.extras?.get(CANDIDATES_EXTRA) as List<RecognisedCandidate>
        adapter.setVisitors(recognisedCandidates)


        btnNoMatch.setOnClickListener {
            intent = Intent(this@VisitorListActivity, RegistrationActivity::class.java)
            startActivity(intent)
        }

    }


    override fun onVisitorItemClicked(visitor:Visitor){
        intent = Intent(this@VisitorListActivity, GreetingActivity::class.java)
        intent.putExtra(VISITOR_EXTRA, visitor);
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
