package de.develappers.facerecognition

import android.app.Application
import android.os.Environment
import android.util.Log
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import de.develappers.facerecognition.utils.APP_MODE_DATABASE
import java.io.File

class FaceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MicrosoftServiceClient =
            FaceServiceRestClient(getString(R.string.microsoft_endpoint), getString(R.string.microsoft_subscription_key))


        storageDirectory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        galleryFolder = File(storageDirectory, applicationContext.getString(R.string.app_name))
        if (!galleryFolder.exists()) {
            val wasCreated: Boolean = galleryFolder.mkdirs()
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory")
            }
        }
    }

    companion object {
        val faceServiceClient: FaceServiceClient?
            get() = MicrosoftServiceClient

        private var MicrosoftServiceClient: FaceServiceClient? = null

        var storageDirectory: File? = null
        lateinit var galleryFolder: File


    }
}