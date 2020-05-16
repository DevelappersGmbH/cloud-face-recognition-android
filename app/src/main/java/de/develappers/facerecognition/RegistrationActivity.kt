package de.develappers.facerecognition

import android.content.DialogInterface
import android.content.Intent
import android.graphics.*
import android.hardware.camera2.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import de.develappers.facerecognition.database.FRdb
import de.develappers.facerecognition.database.dao.VisitorDao
import de.develappers.facerecognition.listeners.OnSignedCaptureListener
import de.develappers.facerecognition.database.model.Visitor
import de.develappers.facerecognition.database.model.Company
import de.develappers.facerecognition.serviceAI.MicrosoftServiceAI
import de.develappers.facerecognition.utils.*
import de.develappers.facerecognition.view.SignatureView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_registration.*
import kotlinx.android.synthetic.main.activity_registration.progressBar
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class RegistrationActivity : CameraActivity(), SignatureView.OnSignedListener, OnSignedCaptureListener {

    private var signed: Boolean = false
    private lateinit var  visitor: Visitor
    private lateinit var currentPhotoPath: String
    private lateinit var visitorDao: VisitorDao
    private lateinit var microsoftServiceAI: MicrosoftServiceAI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        lifecycleScope.launch {
            visitorDao = FRdb.getDatabase(application, this).visitorDao()
        }
        microsoftServiceAI = MicrosoftServiceAI(this)
        textureView = findViewById(R.id.cameraSurface)
        visitor = Visitor(null, null, null, false)
        val company = Company(null)
        visitor.company = company

        etFirstName.addTextChangedListener(textWatcher)
        etLastName.addTextChangedListener(textWatcher)
        etCompany.addTextChangedListener(textWatcher)


        checkBoxPrivacy.setOnCheckedChangeListener{ _, isChecked ->
            visitor.privacyAccepted = isChecked
        }

        btnOk.setOnClickListener {
            currentPhotoPath = createImageFile()
            saveVisitorData()
            if (verifyForm()){
                setProgressBar()
                lockFocus()
            }
        }

        signatureView.setOnSignedListener(this)

        finalCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                unlockFocus()

                lifecycleScope.launch {
                    //save new visitor in database, get visitor id, set visitor id to visitor
                    val newVisitorId = visitorDao.insert(visitor)
                    visitor.visitorId = newVisitorId

                    //also send visitor data to AI services to register
                    val microsoftId = microsoftServiceAI.addNewVisitorToDatabase(VISITORS_GROUP_ID, currentPhotoPath)
                    visitor.microsoftId = microsoftId
                    visitorDao.updateVisitor(visitor)

                    microsoftServiceAI.microsoftTrainPersonGroup(VISITORS_GROUP_ID)

                    navigateToGreeting()
                }
            }
        }

    }

    private fun saveSignatureImage(bitmap: Bitmap) {
        var outputStream:FileOutputStream? = null
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "signature_" + timeStamp + "_"
        try {

            val signatureFile = File(galleryFolder,"$imageFileName.jpg")
            signatureFile.createNewFile()
            outputStream = FileOutputStream(signatureFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        } catch (e: Exception) {
            e.printStackTrace();
        } finally {
            try {
                visitor.sigPaths.add(imageFileName)
                outputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun saveVisitorData(){
        visitor.imgPaths.add(currentPhotoPath)
        visitor.firstName = etFirstName.text.toString();
        visitor.lastName = etLastName.text.toString();
        //TODO: look for company in the database and create id if a new company
        visitor.company!!.companyName = etCompany.text.toString()
        onSignatureCaptured(signatureView.getSignatureBitmap(), "")
    }

    override fun onStartSigning() {
        tvSignature.visibility = GONE
    }

    override fun onSigned() {
        //TODO: think of a better way to signal if the signature was set
        signed = true
    }

    override fun onClear() {
        signed = false
    }


    override fun onSignatureCaptured(bitmap: Bitmap, fileUri: String) {
        //TODO: save the signature somewhere, then prompt to a dialog if the user is sure, then move to next activity
        Toast.makeText(this, "Signature captured", Toast.LENGTH_LONG)
        saveSignatureImage(bitmap)
    }


    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(et: Editable?) {
            when (et) {
                etFirstName.editableText -> {
                    visitor.firstName = et.toString()
                }
                etLastName.editableText -> {
                    visitor.lastName = et.toString()
                }
                etCompany.editableText -> {
                    visitor.company!!.companyName = et.toString()
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // no-op
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // no-op
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }

    private fun verifyForm(): Boolean{
        if (visitor.firstName.isNullOrBlank()){
            showAlertDialog(R.string.first_name)
            return false
        }
        if (visitor.lastName.isNullOrBlank()){
            showAlertDialog(R.string.last_name)
            return false
        }
        if (visitor.company?.companyName.isNullOrBlank()){
            showAlertDialog(R.string.company_name)
            return false
        }

        if(!signed){
            showAlertDialog(R.string.signature)
            return false
        }

        if (!visitor.privacyAccepted){
            showAlertDialog(R.string.privacy_policy)
            return false
        }

        return true
    }
    private fun showAlertDialog(field:Int){
        // build alert dialog
        val dialogBuilder = AlertDialog.Builder(this)

        // set message of alert dialog
        when (field){
            R.string.privacy_policy -> dialogBuilder.setMessage(getString(R.string.pp_required))
            else -> dialogBuilder.setMessage(getString(R.string.field_required, getString(field)))
        }
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton(R.string.ok, DialogInterface.OnClickListener {
                    dialog, id -> dialog.cancel()
            })

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle(getString(R.string.oops))
        // show alert dialog
        alert.show()
    }


    private fun setProgressBar(){
        btnOk.isClickable = false
        progressBar.visibility = VISIBLE
    }

    private fun navigateToGreeting(){
        intent = Intent(this@RegistrationActivity, GreetingActivity::class.java)
        intent.putExtra(VISITOR_EXTRA, visitor)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }


}
