package de.develappers.facerecognition

import android.app.Application
import android.os.Environment
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.rekognition.AmazonRekognition
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import java.io.File


class FaceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MicrosoftServiceClient =
            FaceServiceRestClient(getString(R.string.microsoft_endpoint), BuildConfig.MICROSOFT_KEY)

        AmazonServiceClient =
            AmazonRekognitionClient(BasicAWSCredentials(BuildConfig.AWS_KEY_ID, BuildConfig.AWS_ACCESS_KEY))

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
        val microsoftServiceClient: FaceServiceClient?
            get() = MicrosoftServiceClient
        private var MicrosoftServiceClient: FaceServiceClient? = null

        val amazonServiceClient: AmazonRekognition?
            get() = AmazonServiceClient
        private var AmazonServiceClient: AmazonRekognition? = null


        var storageDirectory: File? = null
        lateinit var galleryFolder: File


    }
}